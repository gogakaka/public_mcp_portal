#!/usr/bin/env node

import { createInterface, Interface } from "readline";
import { program } from "commander";
import http from "http";
import https from "https";
import { URL } from "url";

interface JsonRpcRequest {
  jsonrpc: "2.0";
  id: string | number;
  method: string;
  params?: Record<string, unknown>;
}

interface JsonRpcResponse {
  jsonrpc: "2.0";
  id: string | number | null;
  result?: unknown;
  error?: {
    code: number;
    message: string;
    data?: unknown;
  };
}

interface BridgeOptions {
  serverUrl: string;
  apiKey: string;
  timeout: number;
  verbose: boolean;
}

function log(message: string, verbose: boolean): void {
  if (verbose) {
    process.stderr.write(`[umg-bridge] ${message}\n`);
  }
}

function sendResponse(response: JsonRpcResponse): void {
  const serialized = JSON.stringify(response);
  process.stdout.write(serialized + "\n");
}

function sendError(
  id: string | number | null,
  code: number,
  message: string,
  data?: unknown
): void {
  sendResponse({
    jsonrpc: "2.0",
    id,
    error: { code, message, data },
  });
}

function postRequest(
  url: URL,
  body: string,
  apiKey: string,
  timeoutMs: number
): Promise<{ statusCode: number; body: string }> {
  return new Promise((resolve, reject) => {
    const transport = url.protocol === "https:" ? https : http;

    const options = {
      hostname: url.hostname,
      port: url.port || (url.protocol === "https:" ? 443 : 80),
      path: url.pathname + url.search,
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "text/event-stream, application/json",
        Authorization: `Bearer ${apiKey}`,
        "Content-Length": Buffer.byteLength(body),
      },
      timeout: timeoutMs,
    };

    const req = transport.request(options, (res) => {
      const contentType = res.headers["content-type"] || "";

      if (contentType.includes("text/event-stream")) {
        let sseBuffer = "";
        let resolved = false;

        res.setEncoding("utf8");
        res.on("data", (chunk: string) => {
          sseBuffer += chunk;

          const lines = sseBuffer.split("\n");
          sseBuffer = lines.pop() || "";

          for (const line of lines) {
            if (line.startsWith("data: ")) {
              const data = line.slice(6).trim();
              if (data && !resolved) {
                resolved = true;
                resolve({ statusCode: res.statusCode || 200, body: data });
              }
            }
          }
        });

        res.on("end", () => {
          if (!resolved) {
            if (sseBuffer.startsWith("data: ")) {
              resolve({
                statusCode: res.statusCode || 200,
                body: sseBuffer.slice(6).trim(),
              });
            } else {
              reject(new Error("SSE stream ended without data"));
            }
          }
        });
      } else {
        const chunks: Buffer[] = [];
        res.on("data", (chunk: Buffer) => chunks.push(chunk));
        res.on("end", () => {
          resolve({
            statusCode: res.statusCode || 200,
            body: Buffer.concat(chunks).toString("utf8"),
          });
        });
      }
    });

    req.on("error", reject);
    req.on("timeout", () => {
      req.destroy();
      reject(new Error(`Request timed out after ${timeoutMs}ms`));
    });

    req.write(body);
    req.end();
  });
}

async function handleMessage(
  line: string,
  options: BridgeOptions
): Promise<void> {
  let parsed: JsonRpcRequest;

  try {
    parsed = JSON.parse(line);
  } catch {
    sendError(null, -32700, "Parse error: invalid JSON");
    return;
  }

  if (parsed.jsonrpc !== "2.0" || !parsed.method) {
    sendError(
      parsed.id ?? null,
      -32600,
      "Invalid Request: missing jsonrpc or method"
    );
    return;
  }

  log(`-> ${parsed.method} (id: ${parsed.id})`, options.verbose);

  const url = new URL("/api/mcp", options.serverUrl);

  try {
    const { statusCode, body } = await postRequest(
      url,
      JSON.stringify(parsed),
      options.apiKey,
      options.timeout
    );

    if (statusCode >= 400) {
      log(`<- HTTP ${statusCode}: ${body}`, options.verbose);

      let errorData: unknown;
      try {
        errorData = JSON.parse(body);
      } catch {
        errorData = body;
      }

      sendError(
        parsed.id,
        -32000,
        `Server returned HTTP ${statusCode}`,
        errorData
      );
      return;
    }

    let responseData: unknown;
    try {
      responseData = JSON.parse(body);
    } catch {
      responseData = body;
    }

    if (
      typeof responseData === "object" &&
      responseData !== null &&
      "jsonrpc" in responseData
    ) {
      sendResponse(responseData as JsonRpcResponse);
    } else {
      sendResponse({
        jsonrpc: "2.0",
        id: parsed.id,
        result: responseData,
      });
    }

    log(`<- response for ${parsed.method} (id: ${parsed.id})`, options.verbose);
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    log(`<- error: ${message}`, options.verbose);
    sendError(parsed.id, -32603, `Internal error: ${message}`);
  }
}

function startBridge(options: BridgeOptions): void {
  log(`Starting UMG Bridge`, options.verbose);
  log(`Server URL: ${options.serverUrl}`, options.verbose);
  log(`Timeout: ${options.timeout}ms`, options.verbose);

  const rl: Interface = createInterface({
    input: process.stdin,
    terminal: false,
  });

  rl.on("line", (line: string) => {
    const trimmed = line.trim();
    if (trimmed.length === 0) return;
    handleMessage(trimmed, options).catch((err) => {
      log(`Unhandled error: ${err}`, true);
    });
  });

  rl.on("close", () => {
    log("stdin closed, shutting down", options.verbose);
    process.exit(0);
  });

  process.on("SIGINT", () => {
    log("Received SIGINT, shutting down", options.verbose);
    rl.close();
    process.exit(0);
  });

  process.on("SIGTERM", () => {
    log("Received SIGTERM, shutting down", options.verbose);
    rl.close();
    process.exit(0);
  });
}

program
  .name("umg-bridge")
  .description(
    "Universal MCP Gateway CLI bridge - proxies JSON-RPC over stdio to UMG server via HTTP/SSE"
  )
  .version("0.1.0")
  .requiredOption(
    "--server-url <url>",
    "UMG server base URL",
    "http://localhost:8080"
  )
  .requiredOption("--api-key <key>", "API key for authentication")
  .option("--timeout <ms>", "Request timeout in milliseconds", "30000")
  .option("--verbose", "Enable verbose logging to stderr", false)
  .action((opts) => {
    const options: BridgeOptions = {
      serverUrl: opts.serverUrl,
      apiKey: opts.apiKey,
      timeout: parseInt(opts.timeout, 10),
      verbose: opts.verbose,
    };

    if (!options.apiKey) {
      process.stderr.write(
        "Error: --api-key is required. Generate one from the UMG dashboard.\n"
      );
      process.exit(1);
    }

    if (isNaN(options.timeout) || options.timeout <= 0) {
      process.stderr.write(
        "Error: --timeout must be a positive integer in milliseconds.\n"
      );
      process.exit(1);
    }

    startBridge(options);
  });

program.parse(process.argv);
