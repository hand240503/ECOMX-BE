# Shop Technology API

Backend API cho hệ thống Shop Technology được xây dựng bằng Spring Boot.

## Thông tin dự án

- **Framework**: Spring Boot
- **Database**: MySQL
- **API Prefix**: `/api/v1`

## Tiến độ phát triển

### ✅ Đã hoàn thành

#### 1. Quản lý Address (Địa chỉ người dùng)

Quản lý địa chỉ của người dùng với đầy đủ chức năng CRUD.

**Base URL**: `/api/v1/users/addresses`

##### 1.1. Tạo địa chỉ mới
- **Endpoint**: `POST /api/v1/users/addresses`
- **Description**: Tạo địa chỉ mới cho user hiện tại
- **Request Body**:
```json
{
  "addressLine": "123 Đường ABC",
  "city": "Hà Nội",
  "state": "Hoàn Kiếm",
  "country": "Việt Nam",
  "zipCode": "100000",
  "isDefault": true
}
```
- **Response** (201 Created):
```json
{
  "success": true,
  "message": "Address created successfully",
  "data": {
    "id": 1,
    "addressLine": "123 Đường ABC",
    "city": "Hà Nội",
    "state": "Hoàn Kiếm",
    "country": "Việt Nam",
    "zipCode": "100000",
    "isDefault": true
  },
  "errors": null,
  "metadata": null,
  "timestamp": "2024-01-01T10:00:00"
}
```

##### 1.2. Lấy tất cả địa chỉ
- **Endpoint**: `GET /api/v1/users/addresses`
- **Description**: Lấy danh sách tất cả địa chỉ của user hiện tại
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "Addresses retrieved successfully",
  "data": [
    {
      "id": 1,
      "addressLine": "123 Đường ABC",
      "city": "Hà Nội",
      "state": "Hoàn Kiếm",
      "country": "Việt Nam",
      "zipCode": "100000",
      "isDefault": true
    },
    {
      "id": 2,
      "addressLine": "456 Đường XYZ",
      "city": "Hồ Chí Minh",
      "state": "Quận 1",
      "country": "Việt Nam",
      "zipCode": "700000",
      "isDefault": false
    }
  ],
  "errors": null,
  "metadata": null,
  "timestamp": "2024-01-01T10:00:00"
}
```

##### 1.3. Lấy địa chỉ theo ID
- **Endpoint**: `GET /api/v1/users/addresses/{id}`
- **Description**: Lấy thông tin chi tiết một địa chỉ theo ID
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "Address retrieved successfully",
  "data": {
    "id": 1,
    "addressLine": "123 Đường ABC",
    "city": "Hà Nội",
    "state": "Hoàn Kiếm",
    "country": "Việt Nam",
    "zipCode": "100000",
    "isDefault": true
  },
  "errors": null,
  "metadata": null,
  "timestamp": "2024-01-01T10:00:00"
}
```

##### 1.4. Cập nhật địa chỉ
- **Endpoint**: `PUT /api/v1/users/addresses/{id}`
- **Description**: Cập nhật thông tin địa chỉ
- **Request Body**:
```json
{
  "addressLine": "789 Đường MNO",
  "city": "Hà Nội",
  "state": "Cầu Giấy",
  "country": "Việt Nam",
  "zipCode": "100200",
  "isDefault": false
}
```
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "Address updated successfully",
  "data": {
    "id": 1,
    "addressLine": "789 Đường MNO",
    "city": "Hà Nội",
    "state": "Cầu Giấy",
    "country": "Việt Nam",
    "zipCode": "100200",
    "isDefault": false
  },
  "errors": null,
  "metadata": null,
  "timestamp": "2024-01-01T10:00:00"
}
```

##### 1.5. Xóa địa chỉ
- **Endpoint**: `DELETE /api/v1/users/addresses/{id}`
- **Description**: Xóa địa chỉ theo ID
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "Address deleted successfully",
  "data": null,
  "errors": null,
  "metadata": null,
  "timestamp": "2024-01-01T10:00:00"
}
```

##### 1.6. Đặt địa chỉ mặc định
- **Endpoint**: `PUT /api/v1/users/addresses/{id}/default`
- **Description**: Đặt một địa chỉ làm địa chỉ mặc định (tự động bỏ địa chỉ mặc định cũ)
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "Default address set successfully",
  "data": {
    "id": 2,
    "addressLine": "456 Đường XYZ",
    "city": "Hồ Chí Minh",
    "state": "Quận 1",
    "country": "Việt Nam",
    "zipCode": "700000",
    "isDefault": true
  },
  "errors": null,
  "metadata": null,
  "timestamp": "2024-01-01T10:00:00"
}
```

---

#### 2. Quản lý Danh mục (Category - Danh mục cha và danh mục con)

Quản lý danh mục với cấu trúc cây (parent-child hierarchy).

**Base URL**: `/api/v1/categories`

##### 2.1. Tạo danh mục mới
- **Endpoint**: `POST /api/v1/categories`
- **Description**: Tạo danh mục mới (có thể là danh mục cha hoặc danh mục con)
- **Request Body** (Danh mục cha):
```json
{
  "code": "CAT001",
  "name": "Điện tử",
  "status": 1,
  "parentId": null
}
```
- **Request Body** (Danh mục con):
```json
{
  "code": "CAT002",
  "name": "Điện thoại",
  "status": 1,
  "parentId": 1
}
```
- **Response** (201 Created):
```json
{
  "success": true,
  "message": "Category created successfully!",
  "data": {
    "id": 1,
    "code": "CAT001",
    "name": "Điện tử",
    "status": 1,
    "parentId": null,
    "parentName": null,
    "children": [],
    "childrenCount": 0
  },
  "errors": null,
  "metadata": null,
  "timestamp": "2024-01-01T10:00:00"
}
```

##### 2.2. Lấy tất cả danh mục
- **Endpoint**: `GET /api/v1/categories`
- **Description**: Lấy danh sách tất cả danh mục
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "Category list retrieved successfully!",
  "data": [
    {
      "id": 1,
      "code": "CAT001",
      "name": "Điện tử",
      "status": 1,
      "parentId": null,
      "parentName": null,
      "childrenCount": 2
    },
    {
      "id": 2,
      "code": "CAT002",
      "name": "Điện thoại",
      "status": 1,
      "parentId": 1,
      "parentName": "Điện tử",
      "childrenCount": 0
    }
  ],
  "errors": null,
  "metadata": null,
  "timestamp": "2024-01-01T10:00:00"
}
```

##### 2.3. Lấy danh mục cha (root categories)
- **Endpoint**: `GET /api/v1/categories/roots`
- **Description**: Lấy chỉ các danh mục cha (không có parent)
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "Root categories retrieved successfully",
  "data": [
    {
      "id": 1,
      "code": "CAT001",
      "name": "Điện tử",
      "status": 1,
      "parentId": null,
      "parentName": null,
      "children": [
        {
          "id": 2,
          "code": "CAT002",
          "name": "Điện thoại",
          "status": 1,
          "parentId": 1,
          "parentName": "Điện tử",
          "childrenCount": 0
        }
      ],
      "childrenCount": 2
    }
  ],
  "errors": null,
  "metadata": null,
  "timestamp": "2024-01-01T10:00:00"
}
```

##### 2.4. Lấy danh mục con
- **Endpoint**: `GET /api/v1/categories/parent/{parentId}/children`
- **Description**: Lấy tất cả danh mục con của một danh mục cha
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "Child categories retrieved successfully",
  "data": [
    {
      "id": 2,
      "code": "CAT002",
      "name": "Điện thoại",
      "status": 1,
      "parentId": 1,
      "parentName": "Điện tử",
      "childrenCount": 0
    },
    {
      "id": 3,
      "code": "CAT003",
      "name": "Laptop",
      "status": 1,
      "parentId": 1,
      "parentName": "Điện tử",
      "childrenCount": 0
    }
  ],
  "errors": null,
  "metadata": null,
  "timestamp": "2024-01-01T10:00:00"
}
```

##### 2.5. Lấy danh mục theo ID
- **Endpoint**: `GET /api/v1/categories/{id}`
- **Description**: Lấy thông tin chi tiết một danh mục theo ID (bao gồm children)
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "Category retrieved successfully",
  "data": {
    "id": 1,
    "code": "CAT001",
    "name": "Điện tử",
    "status": 1,
    "parentId": null,
    "parentName": null,
    "children": [
      {
        "id": 2,
        "code": "CAT002",
        "name": "Điện thoại",
        "status": 1,
        "parentId": 1,
        "parentName": "Điện tử",
        "childrenCount": 0
      }
    ],
    "childrenCount": 2
  },
  "errors": null,
  "metadata": null,
  "timestamp": "2024-01-01T10:00:00"
}
```

##### 2.6. Cập nhật danh mục
- **Endpoint**: `PUT /api/v1/categories/{id}`
- **Description**: Cập nhật thông tin danh mục
- **Request Body**:
```json
{
  "code": "CAT001_UPDATED",
  "name": "Điện tử - Cập nhật",
  "status": 1,
  "parentId": null
}
```
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "Category updated successfully",
  "data": {
    "id": 1,
    "code": "CAT001_UPDATED",
    "name": "Điện tử - Cập nhật",
    "status": 1,
    "parentId": null,
    "parentName": null,
    "children": [],
    "childrenCount": 2
  },
  "errors": null,
  "metadata": null,
  "timestamp": "2024-01-01T10:00:00"
}
```

##### 2.7. Xóa danh mục
- **Endpoint**: `DELETE /api/v1/categories/{id}`
- **Description**: Xóa danh mục (chỉ cho phép xóa nếu không có children hoặc products)
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "Category deleted successfully",
  "data": null,
  "errors": null,
  "metadata": null,
  "timestamp": "2024-01-01T10:00:00"
}
```
- **Error Response** (400 Bad Request) - Nếu có children:
```json
{
  "success": false,
  "message": "Failed to delete category: Cannot delete category with children. Please delete or move children first.",
  "data": null,
  "errors": [
    {
      "field": "id",
      "message": "Cannot delete category with children. Please delete or move children first."
    }
  ],
  "metadata": null,
  "timestamp": "2024-01-01T10:00:00"
}
```

---

#### 3. Quản lý Sản phẩm (Product)

Quản lý sản phẩm với tính năng **Multi-Unit Pricing** (Đa đơn vị tính).

**Base URL**: `/api/v1/products`

##### 3.1. Tạo sản phẩm mới (Kèm giá và đơn vị)
- **Endpoint**: `POST /api/v1/products`
- **Description**: Tạo sản phẩm mới và thiết lập các mức giá cho từng đơn vị tính.
- **Request Body**:
```json
{
  "productName": "Bia Tiger",
  "description": "Bia ngon",
  "status": 1,
  "categoryId": 1,
  "prices": [
    {
      "unit_id": 1,
      "current_value": 15000,
      "old_value": 0
    },
    {
      "unit_id": 2,
      "current_value": 350000,
      "old_value": 0
    }
  ],
  "isFeatured": true
}
```
*(Ghi chú: Status dùng Integer: 1=Active, 0=Inactive. Prices là danh sách giá theo từng đơn vị. isFeatured là tùy chọn, mặc định false)*

- **Response** (201 Created):
```json
{
  "success": true,
  "message": "Product created successfully",
  "data": {
    "id": 1,
    "productName": "Bia Tiger",
    "description": "Bia ngon",
    "status": 1,
    "categoryId": 1,
    "categoryName": "Đồ uống",
    "categoryCode": "CAT001"
  },
  "errors": null,
  "metadata": null,
  "timestamp": "2024-01-01T10:00:00"
}
```

##### 3.2. Lấy tất cả sản phẩm
- **Endpoint**: `GET /api/v1/products`
- **Description**: Lấy danh sách sản phẩm (Hỗ trợ phân trang theo con trỏ - Cursor Pagination).
- **Query Parameters**:
    - `limit`: Số lượng sản phẩm (mặc định 10).
    - `lastId`: ID của sản phẩm cuối cùng trong trang trước (để lấy trang tiếp theo). Nếu không truyền, sẽ lấy trang đầu tiên (mới nhất).
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "Products retrieved successfully",
  "data": [
    {
      "id": 1,
      "productName": "Bia Tiger",
      "description": "Bia ngon",
      "status": 1,
      "categoryId": 1,
      "categoryName": "Đồ uống",
      "categoryCode": "CAT001"
    }
  ],
  "errors": null,
  "metadata": null,
  "timestamp": "2024-01-01T10:00:00"
}
```

##### 3.3. Lấy sản phẩm theo ID
- **Endpoint**: `GET /api/v1/products/{id}`
- **Description**: Lấy thông tin chi tiết một sản phẩm theo ID
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "Product retrieved successfully",
  "data": {
    "id": 1,
    "productName": "Bia Tiger",
    "description": "Bia ngon",
    "status": 1,
    "categoryId": 1,
    "categoryName": "Đồ uống",
    "categoryCode": "CAT001"
  },
  "errors": null,
  "metadata": null,
  "timestamp": "2024-01-01T10:00:00"
}
```

##### 3.4. Lấy sản phẩm theo danh mục
- **Endpoint**: `GET /api/v1/products/category/{categoryId}`
- **Description**: Lấy tất cả sản phẩm thuộc một danh mục
- **Response** (200 OK): Tương tự 3.2

##### 3.5. Cập nhật sản phẩm
- **Endpoint**: `PUT /api/v1/products/{id}`
- **Description**: Cập nhật thông tin sản phẩm
- **Request Body**:
```json
{
  "productName": "Bia Tiger Bạc",
  "description": "Mô tả mới",
  "status": 1,
  "categoryId": 1
}
```
*(Lưu ý: Status dùng Integer)*

- **Response** (200 OK): Tương tự 3.3

##### 3.6. Xóa sản phẩm
- **Endpoint**: `DELETE /api/v1/products/{id}`
- **Description**: Xóa sản phẩm theo ID
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "Product deleted successfully",
  "data": null,
  "errors": null,
  "metadata": null,
  "timestamp": "2024-01-01T10:00:00"
}
```

##### 3.7. Lấy danh sách sản phẩm nổi bật
- **Endpoint**: `GET /api/v1/products/featured`
- **Description**: Lấy danh sách các sản phẩm được đánh dấu là nổi bật (`isFeatured = true`).
- **Query Parameters**:
    - `limit`: Số lượng sản phẩm (mặc định 10).
- **Response** (200 OK): Tương tự 3.2

##### 3.8. Lấy danh sách sản phẩm bán chạy
- **Endpoint**: `GET /api/v1/products/best-sellers`
- **Description**: Lấy danh sách sản phẩm bán chạy nhất (dựa trên `soldCount`).
- **Query Parameters**:
    - `limit`: Số lượng sản phẩm (mặc định 10).
- **Response** (200 OK): Tương tự 3.2

---

#### 4. Theo dõi Hành vi Người dùng (Collector Log)

Theo dõi và ghi lại các hành vi của người dùng trên hệ thống.

**Base URL**: `/api/v1/collector-logs`

##### 4.1. Tạo log mới
- **Endpoint**: `POST /api/v1/collector-logs`
- **Description**: Tạo một log entry mới để theo dõi hành vi người dùng
- **Request Body**:
```json
{
  "event": "VIEW_PRODUCT",
  "sessionId": "session_123456",
  "deviceType": "MOBILE",
  "platform": "iOS",
  "metadata": "{\"page\": \"product-detail\", \"duration\": 30}",
  "ipAddress": "192.168.1.1",
  "timestamp": "2024-01-01T10:00:00",
  "productId": 1,
  "userId": 1
}
```
- **Response** (201 Created):
```json
{
  "success": true,
  "message": "Collector log created successfully",
  "data": {
    "id": 1,
    "event": "VIEW_PRODUCT",
    "sessionId": "session_123456",
    "deviceType": "MOBILE",
    "platform": "iOS",
    "metadata": "{\"page\": \"product-detail\", \"duration\": 30}",
    "ipAddress": "192.168.1.1",
    "timestamp": "2024-01-01T10:00:00",
    "productId": 1,
    "productName": "iPhone 15 Pro Max",
    "userId": 1,
    "username": "user123",
    "createdDate": "2024-01-01T10:00:00",
    "modifiedDate": "2024-01-01T10:00:00"
  },
  "errors": null,
  "metadata": null,
  "timestamp": "2024-01-01T10:00:00"
}
```

##### 4.2. Lấy tất cả logs
- **Endpoint**: `GET /api/v1/collector-logs`
- **Description**: Lấy danh sách tất cả collector logs
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "Collector logs retrieved successfully",
  "data": [
    {
      "id": 1,
      "event": "VIEW_PRODUCT",
      "sessionId": "session_123456",
      "deviceType": "MOBILE",
      "platform": "iOS",
      "metadata": "{\"page\": \"product-detail\"}",
      "ipAddress": "192.168.1.1",
      "timestamp": "2024-01-01T10:00:00",
      "productId": 1,
      "productName": "iPhone 15 Pro Max",
      "userId": 1,
      "username": "user123"
    }
  ],
  "errors": null,
  "metadata": {
    "count": 1
  },
  "timestamp": "2024-01-01T10:00:00"
}
```

##### 4.3. Lấy log theo ID
- **Endpoint**: `GET /api/v1/collector-logs/{id}`
- **Description**: Lấy thông tin chi tiết một log theo ID
- **Response** (200 OK): Tương tự như 4.1

##### 4.4. Lấy logs theo User ID
- **Endpoint**: `GET /api/v1/collector-logs/user/{userId}`
- **Description**: Lấy tất cả logs của một user cụ thể
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "Collector logs retrieved successfully",
  "data": [...],
  "errors": null,
  "metadata": {
    "count": 10,
    "userId": 1
  },
  "timestamp": "2024-01-01T10:00:00"
}
```

##### 4.5. Lấy logs theo Product ID
- **Endpoint**: `GET /api/v1/collector-logs/product/{productId}`
- **Description**: Lấy tất cả logs liên quan đến một sản phẩm
- **Response** (200 OK): Tương tự như 4.4 với `productId` trong metadata

##### 4.6. Lấy logs theo Event
- **Endpoint**: `GET /api/v1/collector-logs/event/{event}`
- **Description**: Lấy tất cả logs của một loại event cụ thể (ví dụ: VIEW_PRODUCT, ADD_TO_CART, PURCHASE)
- **Response** (200 OK): Tương tự như 4.4 với `event` trong metadata

##### 4.7. Lấy logs theo Session ID
- **Endpoint**: `GET /api/v1/collector-logs/session/{sessionId}`
- **Description**: Lấy tất cả logs trong một session cụ thể
- **Response** (200 OK): Tương tự như 4.4 với `sessionId` trong metadata

##### 4.8. Lấy logs theo khoảng thời gian
- **Endpoint**: `GET /api/v1/collector-logs/date-range?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59`
- **Description**: Lấy tất cả logs trong một khoảng thời gian
- **Query Parameters**:
    - `startDate`: Ngày bắt đầu (ISO 8601 format)
    - `endDate`: Ngày kết thúc (ISO 8601 format)
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "Collector logs retrieved successfully",
  "data": [...],
  "errors": null,
  "metadata": {
    "count": 50,
    "startDate": "2024-01-01T00:00:00",
    "endDate": "2024-01-31T23:59:59"
  },
  "timestamp": "2024-01-01T10:00:00"
}
```

##### 4.9. Lọc logs với nhiều tiêu chí
- **Endpoint**: `POST /api/v1/collector-logs/filter`
- **Description**: Lọc logs với nhiều tiêu chí kết hợp (hỗ trợ pagination)
- **Request Body**:
```json
{
  "userId": 1,
  "productId": 2,
  "event": "VIEW_PRODUCT",
  "sessionId": "session_123456",
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-01-31T23:59:59",
  "page": 1,
  "size": 10
}
```
- **Response** (200 OK): Tương tự như 4.2 với pagination

##### 4.10. Xóa log
- **Endpoint**: `DELETE /api/v1/collector-logs/{id}`
- **Description**: Xóa một log entry theo ID
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "Collector log deleted successfully",
  "data": null,
  "errors": null,
  "metadata": null,
  "timestamp": "2024-01-01T10:00:00"
}
```

---

#### 5. Quản lý Đánh giá Người dùng (User Rating)

Quản lý các đánh giá explicit của người dùng cho sản phẩm.

**Base URL**: `/api/v1/user-ratings`

##### 5.1. Tạo đánh giá mới
- **Endpoint**: `POST /api/v1/user-ratings`
- **Description**: Tạo đánh giá mới cho một sản phẩm (mỗi user chỉ có thể đánh giá một sản phẩm một lần)
- **Request Body**:
```json
{
  "userId": 1,
  "productId": 1,
  "rating": 5,
  "comment": "Sản phẩm rất tốt, chất lượng cao!"
}
```
- **Response** (201 Created):
```json
{
  "success": true,
  "message": "User rating created successfully",
  "data": {
    "id": 1,
    "userId": 1,
    "username": "user123",
    "productId": 1,
    "productName": "iPhone 15 Pro Max",
    "rating": 5,
    "comment": "Sản phẩm rất tốt, chất lượng cao!",
    "createdDate": "2024-01-01T10:00:00",
    "modifiedDate": "2024-01-01T10:00:00"
  },
  "errors": null,
  "metadata": null,
  "timestamp": "2024-01-01T10:00:00"
}
```
- **Error Response** (400 Bad Request) - Nếu user đã đánh giá sản phẩm này:
```json
{
  "success": false,
  "message": "Failed to create user rating: User has already rated this product. Use update instead.",
  "data": null,
  "errors": [
    {
      "field": "rating",
      "message": "User has already rated this product. Use update instead."
    }
  ],
  "metadata": null,
  "timestamp": "2024-01-01T10:00:00"
}
```

##### 5.2. Lấy tất cả đánh giá
- **Endpoint**: `GET /api/v1/user-ratings`
- **Description**: Lấy danh sách tất cả đánh giá
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "User ratings retrieved successfully",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "username": "user123",
      "productId": 1,
      "productName": "iPhone 15 Pro Max",
      "rating": 5,
      "comment": "Sản phẩm rất tốt!",
      "createdDate": "2024-01-01T10:00:00",
      "modifiedDate": "2024-01-01T10:00:00"
    }
  ],
  "errors": null,
  "metadata": {
    "count": 1
  },
  "timestamp": "2024-01-01T10:00:00"
}
```

##### 5.3. Lấy đánh giá theo ID
- **Endpoint**: `GET /api/v1/user-ratings/{id}`
- **Description**: Lấy thông tin chi tiết một đánh giá theo ID
- **Response** (200 OK): Tương tự như 5.1

##### 5.4. Lấy đánh giá theo User và Product
- **Endpoint**: `GET /api/v1/user-ratings/user/{userId}/product/{productId}`
- **Description**: Lấy đánh giá của một user cụ thể cho một sản phẩm cụ thể
- **Response** (200 OK): Tương tự như 5.1

##### 5.5. Lấy đánh giá theo User ID
- **Endpoint**: `GET /api/v1/user-ratings/user/{userId}`
- **Description**: Lấy tất cả đánh giá của một user
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "User ratings retrieved successfully",
  "data": [...],
  "errors": null,
  "metadata": {
    "count": 5,
    "userId": 1
  },
  "timestamp": "2024-01-01T10:00:00"
}
```

##### 5.6. Lấy đánh giá theo Product ID
- **Endpoint**: `GET /api/v1/user-ratings/product/{productId}`
- **Description**: Lấy tất cả đánh giá của một sản phẩm (kèm thống kê average rating và total ratings)
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "User ratings retrieved successfully",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "username": "user123",
      "productId": 1,
      "productName": "iPhone 15 Pro Max",
      "rating": 5,
      "comment": "Sản phẩm rất tốt!",
      "createdDate": "2024-01-01T10:00:00",
      "modifiedDate": "2024-01-01T10:00:00"
    },
    {
      "id": 2,
      "userId": 2,
      "username": "user456",
      "productId": 1,
      "productName": "iPhone 15 Pro Max",
      "rating": 4,
      "comment": "Tốt nhưng giá hơi cao",
      "createdDate": "2024-01-02T10:00:00",
      "modifiedDate": "2024-01-02T10:00:00"
    }
  ],
  "errors": null,
  "metadata": {
    "count": 2,
    "productId": 1,
    "averageRating": 4.5,
    "totalRatings": 2
  },
  "timestamp": "2024-01-01T10:00:00"
}
```

##### 5.7. Lấy điểm trung bình theo Product ID
- **Endpoint**: `GET /api/v1/user-ratings/product/{productId}/average`
- **Description**: Lấy thống kê điểm trung bình và tổng số đánh giá của một sản phẩm
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "Average rating retrieved successfully",
  "data": {
    "productId": 1,
    "averageRating": 4.5,
    "totalRatings": 2
  },
  "errors": null,
  "metadata": null,
  "timestamp": "2024-01-01T10:00:00"
}
```

##### 5.8. Cập nhật đánh giá
- **Endpoint**: `PUT /api/v1/user-ratings/{id}`
- **Description**: Cập nhật đánh giá (rating và/hoặc comment)
- **Request Body**:
```json
{
  "rating": 4,
  "comment": "Cập nhật: Sản phẩm tốt nhưng giá hơi cao"
}
```
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "User rating updated successfully",
  "data": {
    "id": 1,
    "userId": 1,
    "username": "user123",
    "productId": 1,
    "productName": "iPhone 15 Pro Max",
    "rating": 4,
    "comment": "Cập nhật: Sản phẩm tốt nhưng giá hơi cao",
    "createdDate": "2024-01-01T10:00:00",
    "modifiedDate": "2024-01-01T11:00:00"
  },
  "errors": null,
  "metadata": null,
  "timestamp": "2024-01-01T11:00:00"
}
```

##### 5.9. Xóa đánh giá
- **Endpoint**: `DELETE /api/v1/user-ratings/{id}`
- **Description**: Xóa một đánh giá theo ID
- **Response** (200 OK):
```json
{
  "success": true,
  "message": "User rating deleted successfully",
  "data": null,
  "errors": null,
  "metadata": null,
  "timestamp": "2024-01-01T10:00:00"
}
```

---

## Cấu trúc Response chung

Tất cả các API đều trả về response theo format chuẩn:

```json
{
  "success": boolean,
  "message": "string",
  "data": object | array | null,
  "errors": [
    {
      "field": "string",
      "message": "string"
    }
  ] | null,
  "metadata": object | null,
  "timestamp": "string (ISO 8601 format)"
}
```

### Các trường hợp Response

- **Success (200/201)**: `success = true`, có `data`
- **Error (400/404/500)**: `success = false`, có `errors`, `data = null`

---

## Xác thực

Tất cả các API yêu cầu xác thực qua JWT token trong header:
```
Authorization: Bearer <token>
```

---

## Công nghệ sử dụng

- **Spring Boot**: Framework chính
- **Spring Data JPA**: ORM và database access
- **MySQL**: Database
- **JWT**: Authentication

---

## Hướng dẫn chạy dự án

1. Cài đặt MySQL và tạo database `ShopTechnology`
2. Cấu hình thông tin database trong `application.yml`
3. Chạy ứng dụng:
   ```bash
   ./mvnw spring-boot:run
   ```

---

## Ghi chú

- Tất cả các timestamp trong response theo format ISO 8601
- Các field bắt buộc sẽ được validate và trả về lỗi nếu thiếu
- Xóa danh mục chỉ cho phép nếu không có children hoặc products
