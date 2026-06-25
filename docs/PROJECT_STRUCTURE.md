# Cấu trúc dự án ecomx-be (theo cụm chức năng)

> Tất cả đường dẫn dưới đây tính từ package gốc:
> `src/main/java/com/ndh/ShopTechnology/`
>
> Mỗi cụm chia theo các tầng: **Controller** (tách *Admin* và *User/Public*) →
> **Service** (interface + `impl`) → **Repository** → **Entity** → **DTO** (request/response).
> Quy ước: controller trong `controller/admin/**` là phần **Admin (quản trị)**;
> các controller còn lại là phần **User/Public (người dùng/cửa hàng)**.

---

## 1. Auth — Xác thực & Token

| Tầng | File |
|---|---|
| Controller (User) | `controller/auth/AuthController.java`, `controller/auth/OTPController.java` |
| Service | `services/auth/AuthService.java` → `services/auth/impl/AuthServiceImpl.java`; `services/auth/JwtService.java`, `services/auth/TokenFacade.java`; `services/otp/OTPService.java` → `impl/OTPServiceImpl.java`; `services/token/PasswordResetTokenService.java`, `services/token/RefreshTokenService.java` |
| Repository | `repository/OTPRepository.java`, `repository/PasswordResetTokenRepository.java`, `repository/RefreshTokenRepository.java` |
| Entity | `entities/otp/OTPEntity.java`, `entities/otp/OTPPurpose.java`, `entities/token/PasswordResetTokenEntity.java`, `entities/token/RefreshTokenEntity.java` |
| DTO | `dto/request/auth/**` |

---

## 2. User & Address — Người dùng và Địa chỉ

| Tầng | File |
|---|---|
| Controller (User) | `controller/user/UserController.java`, `controller/user/AddressController.java` |
| Controller (Admin) | `controller/admin/AdminUserController.java`, `controller/admin/AdminCustomerController.java`, `controller/admin/AdminStaffController.java`, `controller/admin/AdminEmployeeController.java` |
| Service | `services/user/UserService.java` → `impl/UserServiceImpl.java`; `UserAuthService` → `impl/UserAuthServiceImpl.java`; `AddressService` → `impl/AddressServiceImpl.java`; `RoleAssignmentService`, `UserRoleService`, `UserAvatarService`, `CustomUserDetailsService`, `helper/UserValidationHelper.java` |
| Repository | `repository/UserRepository.java`, `repository/UserAddressRepository.java`, `repository/UserPermissionRepository.java`, `repository/UserDepartmentRepository.java` |
| Entity | `entities/user/UserEntity.java`, `UserInfoEntity.java`, `UserAddressEntity.java`, `AddressType.java`, `UserPermissionEntity.java`, `entities/key/KeyUsers.java` |
| DTO | `dto/request/user/**`, `dto/response/user/**` |
| Hỗ trợ | `services/user/UserAvatarService.java` (avatar), `enums/UserState.java` |

---

## 3. Role / Permission / Department — Phân quyền & Phòng ban

| Tầng | File |
|---|---|
| Controller (Admin) | `controller/admin/RolePermissionController.java`, `controller/admin/AdminDepartmentController.java` |
| Service | `services/permission/PermissionService.java`, `RolePermissionService.java` → `impl/RolePermissionServiceImpl.java`, `PermissionEvaluator.java`, `PermissionAuthorizationBean.java`; `services/department/DepartmentService.java` → `impl/DepartmentServiceImpl.java` |
| Repository | `repository/RoleRepository.java`, `repository/UserPermissionRepository.java`, `repository/DepartmentRepository.java`, `repository/UserDepartmentRepository.java` |
| Entity | `entities/role/RoleEntity.java`, `entities/user/UserPermissionEntity.java`, `entities/department/DepartmentEntity.java`, `UserDepartmentEntity.java` |
| DTO | `dto/request/role/**`, `dto/request/permission/**`, `dto/request/department/**`; `dto/response/role/**`, `permission/**`, `department/**` |
| Constants | `constants/RoleConstant.java`, `PermissionCode.java`, `PermissionDescriptions.java`, `RolePermissionDefaults.java`; bootstrap `config/RolePermissionBootstrapper.java` |

---

## 4. Product Catalog — Sản phẩm, Danh mục, Thương hiệu, Đơn vị

| Tầng | File |
|---|---|
| Controller (User) | `controller/product/ProductController.java`, `controller/category/CategoryController.java` |
| Controller (Admin) | `controller/admin/AdminProductController.java`, `AdminCategoryController.java`, `AdminBrandController.java`, `AdminUnitController.java` |
| Service | `services/product/ProductService.java` → `impl/ProductServiceImpl.java`; phụ trợ: `impl/ProductImageAttachService.java`, `ProductVariantPriceHydrator.java`, `VariantImageAttachService.java`, `VariantDisplayPriceResolver.java`, `ProductPricingProgramsAttachService.java`; `services/brand/BrandService.java`(+impl), `services/category/CategoryService.java`(+impl), `services/unit/UnitService.java`(+impl) |
| Repository | `repository/ProductRepository.java`, `ProductVariantRepository.java`, `BrandRepository.java`, `CategoryRepository.java`, `UnitRepository.java`, `PolicyRepository.java`, `PriceRepository.java`, `spec/ProductSearchSpecifications.java`, `projection/ProductRatingAggregate.java` |
| Entity | `entities/product/ProductEntity.java`, `ProductVariantEntity.java`, `BrandEntity.java`, `CategoryEntity.java`, `UnitEntity.java`, `PolicyEntity.java`, `PolicyType.java`, `PriceEntity.java` |
| DTO | `dto/request/{product,brand,category,unit}/**`; `dto/response/{product,brand,category,unit}/**` |

---

## 5. Pricing & Promotion — Giá & Khuyến mãi

| Tầng | File |
|---|---|
| Controller (Admin) | `controller/admin/AdminProductPriceController.java`, `AdminProductPriceChangeController.java`, `AdminVolumePriceTierController.java`, `AdminPurchaseWithPurchaseController.java` |
| Service | `services/product/ProductPriceService.java`(+`impl/ProductPriceServiceImpl.java`), `ProductPriceChangeService.java`(+impl), `ProductEffectivePriceService.java`(+impl); `services/promotion/PromotionPricingService.java`(+impl), `ProductVolumePriceTierService.java`(+impl), `PurchaseWithPurchaseOfferService.java`(+impl) |
| Repository | `repository/PriceRepository.java`, `ProductPriceChangeRepository.java`, `ProductPriceChangeUsageRepository.java`, `ProductVolumePriceTierRepository.java`, `PurchaseWithPurchaseOfferRepository.java` |
| Entity | `entities/product/PriceEntity.java`, `ProductPriceChangeEntity.java`, `ProductPriceChangeUsageEntity.java`; `entities/promotion/ProductVolumePriceTierEntity.java`, `PurchaseWithPurchaseOfferEntity.java` |
| DTO | `dto/request/promotion/**`, `dto/response/promotion/**` |

---

## 6. Order / Checkout / Payment — Đơn hàng, Thanh toán

| Tầng | File |
|---|---|
| Controller (User) | `controller/order/OrderController.java`, `controller/order/PaymentMethodController.java`, `controller/payment/VnpayController.java` |
| Controller (Admin) | `controller/admin/AdminOrderController.java` |
| Service | `services/order/OrderService.java` → `impl/OrderServiceImpl.java`; `PaymentMethodService.java` → `impl/PaymentMethodServiceImpl.java`; `VnpaySessionFinalizeResult.java`; `services/payment/VnpayService.java`(+impl), `VnpayQueryDrService.java`(+impl), `VnpayReconcileScheduler.java` |
| Repository | `repository/OrderRepository.java`, `OrderDetailRepository.java`, `CheckoutSessionRepository.java`, `PaymentMethodRepository.java`, `OrderHistoryRepository.java` |
| Entity | `entities/order/OrderEntity.java`, `OrderDetailEntity.java`, `CheckoutSessionEntity.java`, `CheckoutSessionStatus.java`, `PaymentMethodEntity.java` |
| DTO | `dto/request/order/**`; `dto/response/{order,payment}/**` |
| Constants/Config | `constants/OrderConstants.java`; `config/VnpayProperties.java`, `PaymentMethodDataLoader.java`, `VnpayPaymentMethodEnsurer.java` |

---

## 7. Inventory / Warehouse — Kho hàng (đồng bộ tồn kho) ⭐ mới

> Tồn kho ở cấp **biến thể (ProductVariant)**. Phần này **đồng bộ qua luồng đơn hàng**
> (giữ khi đặt, xuất khi hoàn thành, nhập lại khi hoàn tiền) nên không có controller phía User riêng.

| Tầng | File |
|---|---|
| Controller (Admin) | `controller/admin/AdminInventoryController.java` (nhập kho / điều chỉnh / xem tồn + sổ cái) |
| Đồng bộ (User) | tích hợp trong `services/order/impl/OrderServiceImpl.java` (reserve / release / sale-out / restock) |
| Service | `services/inventory/InventoryService.java` → `impl/InventoryServiceImpl.java` |
| Repository | `repository/InventoryLedgerRepository.java`; `ProductVariantRepository.java` (UPDATE atomic tồn); `ProductRepository.java` (soldCount) |
| Entity | `entities/inventory/InventoryLedgerEntity.java`; `entities/product/ProductVariantEntity.java` (`onHand`, `reserved`) |
| Enum | `enums/inventory/InventoryMovementType.java` |
| DTO | `dto/request/inventory/**`, `dto/response/inventory/**` |
| Tài liệu | `docs/QUAN_LY_KHO_HANG.md` |

---

## 8. Comment & Rating — Bình luận & Đánh giá

| Tầng | File |
|---|---|
| Controller (User) | `controller/comment/ProductCommentController.java`, `controller/rating/UserRatingController.java` |
| Controller (Admin) | `controller/admin/AdminProductCommentController.java` |
| Service | `services/comment/ProductCommentService.java`(+impl); `services/rating/UserRatingService.java`(+impl) |
| Repository | `repository/ProductCommentRepository.java`, `repository/UserRatingRepository.java`, `repository/projection/ProductRatingAggregate.java` |
| Entity | `entities/comment/ProductCommentEntity.java`, `entities/rating/UserRatingEntity.java` |
| DTO | `dto/request/{comment,rating}/**`, `dto/response/{comment,rating}/**` |
| Constants | `constants/RatingConstants.java` |

---

## 9. Recommendation & Search — Gợi ý & Tìm kiếm

| Tầng | File |
|---|---|
| Controller (User) | `controller/recommendation/RecommendationController.java`, `controller/search/SearchController.java` |
| Service | `services/recommendation/**` (CB/CF/Hybrid/Popularity/SessionBased/Enrichment/UserState… + `impl/**`, `dto/**`); `services/search/SearchTrendingService.java` |
| Repository | `repository/CbContentRecommendationRepository.java`, `repository/ItemSimilarityRepository.java`, `repository/PopularityRepository.java` |
| Entity | `entities/recommendation/CbContentRecommendation.java`, `ItemSimilarityEntity.java` |
| DTO | `dto/response/{recommendation,search}/**` |
| Constants/Config | `constants/RecommendationAlgorithm.java`; `config/RecommendationBlendProperties.java`, `RecommendationDiversityProperties.java` |

---

## 10. Notification — Thông báo

| Tầng | File |
|---|---|
| Controller (User) | `controller/notification/NotificationController.java` |
| Service | `services/notification/NotificationService.java` → `impl/NotificationServiceImpl.java` |
| Repository | `repository/NotificationRepository.java` |
| Entity | `entities/notification/NotificationEntity.java` |
| DTO | `dto/response/notification/**` |

---

## 11. Document — Tài liệu

| Tầng | File |
|---|---|
| Controller (User) | `controller/document/DocumentController.java`, `controller/document/DocumentUploadRequestChecks.java` |
| Controller (Admin) | `controller/admin/AdminDocumentController.java` |
| Service | `services/doc/DocumentService.java` → `impl/DocumentServiceImpl.java` |
| Repository | `repository/DocumentRepository.java` |
| Entity | `entities/doc/DocumentEntity.java` |
| Constants | `constants/DocumentEntityType.java`, `DocumentKind.java` |

---

## 12. Task / Kanban — Quản lý công việc (backend còn, UI đã ẩn)

> Đã ẩn khỏi giao diện admin; mã backend giữ nguyên làm code chết.

| Tầng | File |
|---|---|
| Controller (Admin) | `controller/admin/task/AdminTaskController.java`, `TaskAttachmentController.java`, `TaskNotificationController.java` |
| Service | `services/task/TaskService.java` → `impl/TaskServiceImpl.java`; `TaskActivityLogService.java`, `TaskAttachmentService.java`, `TaskNotificationService.java`, `MentionParser.java`; event: `OrderTaskEventListener.java`, `OrderCreatedEvent.java`, `OrderCancelledEvent.java`, `OrderReturnedEvent.java` |
| Repository | `repository/task/**` (Task, KanbanBoard, KanbanColumn, TaskActivityLog, TaskAttachment, TaskComment, TaskMention, TaskNotification, TaskParticipant, TaskStatusHistory) |
| Entity | `entities/task/**` (TaskEntity, KanbanBoardEntity, KanbanColumnEntity, TaskActivityLogEntity, TaskAttachmentEntity, TaskChecklistEntity, TaskChecklistItemEntity, TaskCommentEntity, TaskMentionEntity, TaskNotificationEntity, TaskParticipantEntity, TaskStatusHistoryEntity) |
| DTO / Enum | `dto/request/task/**`, `dto/response/task/**`; `enums/task/**` |

---

## 13. Logging / History / Report / Job — Nhật ký, Lịch sử, Báo cáo

| Tầng | File |
|---|---|
| Controller (Admin) | `controller/admin/AdminHistoryController.java`, `controller/admin/AdminReportController.java` |
| Controller (khác) | `controller/log/CollectorLogController.java`, `controller/job/JobReportController.java` |
| Service | `services/log/**` (AdminActivityLogService, OrderHistoryService, PriceEventHistoryService, CollectorLogService, UnifiedHistoryService + `impl/**`); `services/report/ReportService.java`(+Impl); `services/job/JobReportService.java`(+impl) |
| Repository | `repository/AdminActivityLogRepository.java`, `OrderHistoryRepository.java`, `PriceEventHistoryRepository.java`, `CollectorLogRepository.java`, `JobReportRepository.java`, `JobReportDetailRepository.java` |
| Entity | `entities/log/**` (AdminActivityLogEntity, CollectorLogEntity, OrderHistoryEntity, PriceEventHistoryEntity); `entities/job/**` |
| DTO/AOP | `dto/{request,response}/{log,report}/**`; `aspect/AdminActivityLogAspect.java`, `aspect/SnapshotFetcherRegistry.java`, `annotation/AdminAudit.java`; `constants/CollectorLogEventConstant.java` |

---

## 14. Delivery / Shipping — Giao hàng

| Tầng | File |
|---|---|
| Controller (User) | `controller/delivery/ShippingDistanceController.java` |
| Service | `services/delivery/DeliveryRoutingService.java` |
| Util/Config | `utils/ShippingFeeCalculator.java`; `config/DeliveryRoutingProperties.java`, `config/DefaultWarehouseAddressLoader.java` |
| DTO | `dto/response/delivery/**` |

---

## 15. Hạ tầng dùng chung (Cross-cutting / Infrastructure)

| Nhóm | File |
|---|---|
| Security & Filter | `config/WebSecurityConfig.java`, `JwtAuthenticationEntryPoint.java`, `TokenProvider.java`, `MyUser.java`; `filters/**` |
| Cấu hình | `config/AsyncConfig.java`, `CacheConfig.java`, `JpaAuditingConfig.java`, `RestTemplateConfig.java`, `MailConfig.java`, `CloudinaryConfig.java`, `UploadStorageProperties.java`, `TokenCleanupScheduler.java` |
| Dịch vụ chung | `services/common/CommonService.java`, `APIAuthService.java`; `services/redis/RedisService.java`, `UserRedisService.java`; `services/email/EmailService.java`; `services/storage/CloudinaryService.java` |
| Exception | `exception/GlobalExceptionHandler.java`, `CustomApiException.java`, `NotFoundEntityException.java`, `AuthenticationFailedException.java` |
| Base & chung | `entities/BaseEntity.java`, `entities/config/ConfigEntity.java`; `dto/response/APIResponse.java`, `ErrorResponse.java`, `PaginationMetadata.java`, `ResultPagination.java`; `dto/request/PaginationRequest.java`; `constants/SystemConstant.java`, `MessageConstant.java`; `utils/FileUtils.java`, `utils/CatalogVariantUnitPrice.java`, `utils/ProductCatalogListing.java` |

---

### Ghi chú đọc nhanh

- **Luồng request điển hình:** `Controller` → `Service (interface)` → `…ServiceImpl` → `Repository` → `Entity`; dữ liệu vào/ra qua `dto/request` và `dto/response`.
- **Admin vs User:** phân biệt bằng đường dẫn controller (`controller/admin/**` = admin). Cùng một cụm thường dùng chung Service/Repository/Entity, chỉ khác controller và một số DTO/endpoint.
- **Kho hàng** không có controller User riêng — nó được đồng bộ tự động bên trong `OrderServiceImpl` ở các mốc trạng thái đơn hàng.
