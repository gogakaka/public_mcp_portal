package com.umg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umg.domain.entity.CubeDataSource;
import com.umg.domain.entity.User;
import com.umg.domain.enums.CubeDbType;
import com.umg.domain.enums.DataSourceStatus;
import com.umg.dto.CubeDataSourceDto;
import com.umg.repository.CubeDataSourceRepository;
import com.umg.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CubeDataSourceService 단위 테스트")
class CubeDataSourceServiceTest {

    @Mock
    private CubeDataSourceRepository cubeDataSourceRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CubeDataSourceService service;

    private UUID userId;
    private UUID dataSourceId;
    private CubeDataSource sampleDataSource;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        dataSourceId = UUID.randomUUID();

        sampleUser = new User();
        sampleUser.setId(userId);
        sampleUser.setName("테스트 사용자");

        sampleDataSource = CubeDataSource.builder()
                .id(dataSourceId)
                .name("test-ds")
                .description("테스트 데이터소스")
                .dbType(CubeDbType.POSTGRESQL)
                .connectionConfig("{\"host\":\"localhost\",\"port\":5432,\"database\":\"testdb\",\"username\":\"user\",\"password\":\"pass\"}")
                .status(DataSourceStatus.ACTIVE)
                .createdBy(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("유효한 요청으로 데이터소스를 생성할 수 있다")
        void shouldCreateDataSourceWithValidRequest() {
            CubeDataSourceDto.CreateRequest request = new CubeDataSourceDto.CreateRequest(
                    "new-ds", "설명", CubeDbType.POSTGRESQL, "localhost", 5432, "mydb", "user", "pass"
            );

            when(cubeDataSourceRepository.save(any(CubeDataSource.class))).thenAnswer(inv -> {
                CubeDataSource ds = inv.getArgument(0);
                ds.setId(dataSourceId);
                ds.setCreatedAt(Instant.now());
                ds.setUpdatedAt(Instant.now());
                return ds;
            });
            when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

            CubeDataSourceDto.Response result = service.create(request, userId);

            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("new-ds");
            assertThat(result.dbType()).isEqualTo(CubeDbType.POSTGRESQL);
            assertThat(result.status()).isEqualTo(DataSourceStatus.ACTIVE);
            assertThat(result.creatorName()).isEqualTo("테스트 사용자");

            ArgumentCaptor<CubeDataSource> captor = ArgumentCaptor.forClass(CubeDataSource.class);
            verify(cubeDataSourceRepository).save(captor.capture());
            CubeDataSource saved = captor.getValue();
            assertThat(saved.getCreatedBy()).isEqualTo(userId);
            assertThat(saved.getConnectionConfig()).contains("localhost");
        }

        @Test
        @DisplayName("connectionConfig에 host, port, database, username, password가 포함된다")
        void shouldBuildConnectionConfigCorrectly() throws Exception {
            CubeDataSourceDto.CreateRequest request = new CubeDataSourceDto.CreateRequest(
                    "ds", null, CubeDbType.MYSQL, "db.example.com", 3306, "app", "admin", "secret"
            );

            when(cubeDataSourceRepository.save(any(CubeDataSource.class))).thenAnswer(inv -> {
                CubeDataSource ds = inv.getArgument(0);
                ds.setId(dataSourceId);
                ds.setCreatedAt(Instant.now());
                ds.setUpdatedAt(Instant.now());
                return ds;
            });
            when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

            service.create(request, userId);

            ArgumentCaptor<CubeDataSource> captor = ArgumentCaptor.forClass(CubeDataSource.class);
            verify(cubeDataSourceRepository).save(captor.capture());

            String config = captor.getValue().getConnectionConfig();
            var configMap = objectMapper.readValue(config, java.util.Map.class);
            assertThat(configMap.get("host")).isEqualTo("db.example.com");
            assertThat(configMap.get("port")).isEqualTo(3306);
            assertThat(configMap.get("database")).isEqualTo("app");
            assertThat(configMap.get("username")).isEqualTo("admin");
            assertThat(configMap.get("password")).isEqualTo("secret");
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("이름만 변경하면 connectionConfig는 그대로 유지된다")
        void shouldUpdateNameOnly() {
            CubeDataSourceDto.UpdateRequest request = new CubeDataSourceDto.UpdateRequest(
                    "updated-name", null, null, null, null, null, null
            );

            when(cubeDataSourceRepository.findById(dataSourceId)).thenReturn(Optional.of(sampleDataSource));
            when(cubeDataSourceRepository.save(any(CubeDataSource.class))).thenReturn(sampleDataSource);
            when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

            CubeDataSourceDto.Response result = service.update(dataSourceId, request);

            assertThat(result).isNotNull();
            verify(cubeDataSourceRepository).save(argThat(ds ->
                    ds.getName().equals("updated-name")
            ));
        }

        @Test
        @DisplayName("연결 정보 변경 시 connectionConfig가 재구성된다")
        void shouldRebuildConnectionConfigWhenConnectionFieldsChange() {
            CubeDataSourceDto.UpdateRequest request = new CubeDataSourceDto.UpdateRequest(
                    null, null, "newhost.com", null, null, null, null
            );

            when(cubeDataSourceRepository.findById(dataSourceId)).thenReturn(Optional.of(sampleDataSource));
            when(cubeDataSourceRepository.save(any(CubeDataSource.class))).thenReturn(sampleDataSource);
            when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

            service.update(dataSourceId, request);

            verify(cubeDataSourceRepository).save(argThat(ds ->
                    ds.getConnectionConfig().contains("newhost.com")
            ));
        }

        @Test
        @DisplayName("존재하지 않는 데이터소스를 수정하면 예외가 발생한다")
        void shouldThrowExceptionForNonExistentDataSource() {
            UUID nonExistentId = UUID.randomUUID();
            CubeDataSourceDto.UpdateRequest request = new CubeDataSourceDto.UpdateRequest(
                    "name", null, null, null, null, null, null
            );

            when(cubeDataSourceRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(nonExistentId, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("데이터소스를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("존재하는 데이터소스를 삭제할 수 있다")
        void shouldDeleteExistingDataSource() {
            when(cubeDataSourceRepository.findById(dataSourceId)).thenReturn(Optional.of(sampleDataSource));

            service.delete(dataSourceId);

            verify(cubeDataSourceRepository).delete(sampleDataSource);
        }

        @Test
        @DisplayName("존재하지 않는 데이터소스를 삭제하면 예외가 발생한다")
        void shouldThrowExceptionForNonExistentDataSource() {
            UUID nonExistentId = UUID.randomUUID();
            when(cubeDataSourceRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(nonExistentId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("데이터소스를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("ID로 데이터소스를 조회할 수 있다")
        void shouldFindDataSourceById() {
            when(cubeDataSourceRepository.findById(dataSourceId)).thenReturn(Optional.of(sampleDataSource));
            when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

            CubeDataSourceDto.Response result = service.findById(dataSourceId);

            assertThat(result.id()).isEqualTo(dataSourceId);
            assertThat(result.name()).isEqualTo("test-ds");
            assertThat(result.creatorName()).isEqualTo("테스트 사용자");
        }
    }

    @Nested
    @DisplayName("findAll() / findByStatus()")
    class Find {

        @Test
        @DisplayName("모든 데이터소스를 페이징하여 조회할 수 있다")
        void shouldFindAllPaged() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<CubeDataSource> page = new PageImpl<>(List.of(sampleDataSource), pageable, 1);

            when(cubeDataSourceRepository.findAll(pageable)).thenReturn(page);
            when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

            Page<CubeDataSourceDto.Response> result = service.findAll(pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).name()).isEqualTo("test-ds");
        }

        @Test
        @DisplayName("상태별 데이터소스를 필터링하여 조회할 수 있다")
        void shouldFindByStatus() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<CubeDataSource> page = new PageImpl<>(List.of(sampleDataSource), pageable, 1);

            when(cubeDataSourceRepository.findByStatus(DataSourceStatus.ACTIVE, pageable)).thenReturn(page);
            when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

            Page<CubeDataSourceDto.Response> result = service.findByStatus(DataSourceStatus.ACTIVE, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }
}
