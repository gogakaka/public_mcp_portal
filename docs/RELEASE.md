# 릴리즈 가이드 - Universal MCP Gateway (UMG)

## 버전 관리

UMG는 [시맨틱 버저닝](https://semver.org/lang/ko/)을 따릅니다:

- **MAJOR** (X.0.0): 호환성이 깨지는 API 변경, 데이터 변환이 필요한 스키마 마이그레이션
- **MINOR** (0.X.0): 새로운 기능, 하위 호환 API 추가
- **PATCH** (0.0.X): 버그 수정, 보안 패치

---

## 릴리즈 프로세스

### 1. 릴리즈 전 체크리스트

```bash
# 모든 테스트 통과 확인
cd backend && ./gradlew test
cd frontend && npm run lint && npm run build

# 보안 취약점 검사
cd backend && ./gradlew dependencyCheckAnalyze
cd frontend && npm audit

# 데이터베이스 마이그레이션 일관성 확인
cd backend && ./gradlew flywayValidate
```

### 2. 버전 업데이트

#### 백엔드

`backend/build.gradle` 편집:
```groovy
version = '1.1.0'  // 버전 업데이트
```

#### 프론트엔드

```bash
cd frontend
npm version minor  # 또는 major/patch
```

### 3. 릴리즈 브랜치 생성

```bash
git checkout -b release/v1.1.0
git add -A
git commit -m "chore: 버전 1.1.0으로 업데이트"
git push origin release/v1.1.0
```

### 4. Docker 이미지 빌드

```bash
# 백엔드 이미지 빌드
docker build -t umg-backend:1.1.0 ./backend
docker tag umg-backend:1.1.0 your-registry.com/umg-backend:1.1.0
docker tag umg-backend:1.1.0 your-registry.com/umg-backend:latest

# 프론트엔드 이미지 빌드
docker build -t umg-frontend:1.1.0 ./frontend
docker tag umg-frontend:1.1.0 your-registry.com/umg-frontend:1.1.0
docker tag umg-frontend:1.1.0 your-registry.com/umg-frontend:latest

# 레지스트리에 푸시
docker push your-registry.com/umg-backend:1.1.0
docker push your-registry.com/umg-backend:latest
docker push your-registry.com/umg-frontend:1.1.0
docker push your-registry.com/umg-frontend:latest
```

### 5. Git 태그 생성

```bash
git tag -a v1.1.0 -m "릴리즈 v1.1.0: 변경사항 설명"
git push origin v1.1.0
```

### 6. 메인 브랜치에 병합

```bash
# release/v1.1.0에서 main으로 PR 생성
gh pr create --title "릴리즈 v1.1.0" --body "릴리즈 노트 내용"

# 승인 후 병합
gh pr merge --merge
```

---

## 배포

### Docker Compose (단일 서버)

```bash
# 최신 이미지 가져오기
docker compose pull

# 무중단 배포
docker compose up -d --no-deps backend
docker compose up -d --no-deps frontend
```

### Kubernetes (EKS/GKE)

#### Helm으로 배포

```bash
helm upgrade --install umg ./helm/umg \
  --namespace umg \
  --set backend.image.tag=1.1.0 \
  --set frontend.image.tag=1.1.0 \
  --set postgres.password=$DB_PASSWORD \
  --set backend.env.UMG_AES_SECRET_KEY=$AES_KEY \
  --set backend.env.UMG_JWT_SECRET=$JWT_SECRET \
  --wait --timeout 5m
```

#### 롤백

```bash
helm rollback umg --namespace umg
```

---

## 릴리즈 체크리스트

### 릴리즈 전

- [ ] 모든 테스트 통과 (백엔드 + 프론트엔드)
- [ ] 코드 리뷰 완료
- [ ] 보안 검사 통과
- [ ] 스테이징에서 마이그레이션 테스트
- [ ] 문서 업데이트 완료

### 릴리즈 중

- [ ] 데이터베이스 백업 완료
- [ ] Docker 이미지 빌드 및 푸시
- [ ] Git 태그 생성
- [ ] 헬스체크 통과

### 릴리즈 후

- [ ] 30분간 오류율 모니터링
- [ ] 프로덕션에서 주요 흐름 검증
- [ ] 이해관계자 알림
- [ ] 릴리즈 브랜치 병합

---

## 핫픽스 프로세스

```bash
# 최신 태그에서 핫픽스 브랜치 생성
git checkout -b hotfix/v1.1.1 v1.1.0

# 수정 후 커밋
git commit -am "fix: 긴급 이슈 설명"
git push origin hotfix/v1.1.1

# 빌드, 배포 후 main에 병합
```
