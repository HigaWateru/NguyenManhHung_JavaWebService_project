# Luồng unit test cho tầng Controller và Service

Tài liệu này mô tả bộ test mới dùng `JUnit 5`, `Mockito` và báo cáo độ bao phủ bằng `JaCoCo`.

## 1) Mục tiêu

- Bổ sung ít nhất 5 unit test cho tầng Controller.
- Bổ sung ít nhất 5 unit test cho tầng Service.
- Tạo report coverage để theo dõi chất lượng test.

## 2) Phạm vi test đã thêm

### 2.1 Controller test

File: `src/test/java/demo/project/controller/AdminUserControllerTest.java`

5 test case:
1. `searchShouldReturnPageResponse`
2. `createShouldReturnCreated`
3. `updateShouldReturnOk`
4. `deleteShouldReturnNoContent`
5. `createShouldReturnBadRequestWhenPayloadInvalid`

Kỹ thuật dùng:
- `MockMvc` standalone để test HTTP layer của controller.
- `Mockito` để mock `UserService`.
- Validation request body bằng `LocalValidatorFactoryBean`.

### 2.2 Service test

File: `src/test/java/demo/project/service/impl/UserServiceImplTest.java`

5 test case:
1. `searchUsersShouldMapPageResult`
2. `createShouldThrowConflictWhenUsernameExists`
3. `createShouldSaveEncodedPassword`
4. `updateShouldApplyChangesAndEncodePassword`
5. `deleteShouldThrowNotFoundWhenUserMissing`

Kỹ thuật dùng:
- `@ExtendWith(MockitoExtension.class)`
- Mock repository và `PasswordEncoder`.
- Verify hành vi save/update/delete và nhánh lỗi nghiệp vụ.

## 3) Cấu hình report JaCoCo

Đã bật plugin JaCoCo trong `build.gradle`:
- plugin `jacoco`
- `test` sẽ tự chạy tiếp `jacocoTestReport`
- report bật `html` và `xml`

## 4) Cách chạy test và sinh report

```powershell
Set-Location "D:\code\Rest API\project"
.\gradlew.bat clean test jacocoTestReport
```

Sau khi chạy xong, mở report tại:
- `build/reports/jacoco/test/html/index.html`

## 5) Luồng thực thi test (tóm tắt)

1. Gradle chạy `test` task với JUnit Platform.
2. Mockito tạo mock cho dependency ở từng test class.
3. Test xác nhận response/controller contract và service business rule.
4. JaCoCo thu thập execution data trong lúc chạy test.
5. Task `jacocoTestReport` sinh report HTML/XML để theo dõi coverage.

