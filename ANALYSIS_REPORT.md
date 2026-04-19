# Báo cáo Phân tích Yêu cầu & Hiện trạng — Metric System

> **Ngày phân tích:** 11/04/2026  
> **Tài liệu yêu cầu:** Phân tích chức năng phần mềm may đo.xlsx  
> **Codebase:** `src/main/java/com/tailorshop/metric`

---

## I. HIỆN TRẠNG HỆ THỐNG

### 1.1 Entities / Database Tables

| Entity | Mô tả | Trạng thái |
|--------|-------|-----------|
| `User` | Người dùng, OAuth2 (Google/Azure/LOCAL), profile, status | ✅ Có |
| `UserRole` | Vai trò: ADMIN, USER, MANAGER | ✅ Có |
| `Customer` | Thông tin khách hàng, CCCD, ngày sinh, giới tính | ✅ Có |
| `TailoringOrder` | Đơn hàng, trạng thái, loại đơn, giá | ✅ Có |
| `Measurement` | Thông số đo lường liên kết đơn hàng | ✅ Có |
| `MeasurementTemplate` | Mẫu đo chuẩn theo loại sản phẩm | ✅ Có |
| `MeasurementField` | Các trường trong mẫu đo | ✅ Có |
| `Payment` | Thanh toán từng lần, nhiều phương thức | ✅ Có |
| `Settings` | Cấu hình hệ thống dạng key-value | ✅ Có |
| `AuditLog` | Nhật ký thay đổi entity (JSONB old/new values) | ✅ Có |
| `ApiAccessLog` | Nhật ký truy cập API | ✅ Có |

### 1.2 API Endpoints Hiện có

**Authentication**
- `POST /auth/login` — Đăng nhập username/password
- `POST /auth/signup` — Đăng ký user mới
- `POST /auth/logout` — Đăng xuất
- `GET /oauth2/authorization/google` — OAuth2 Google
- `GET /oauth2/authorization/azure` — OAuth2 Azure

**Customers** — CRUD, tìm kiếm, phân trang, deactivate  
**Tailoring Orders** — CRUD, filter theo customer/status/date  
**Measurements** — CRUD, lấy theo order  
**Payments** — CRUD, lấy theo order  
**Reports** — Doanh thu, khách hàng, đơn hàng, dashboard summary  
**Settings** — CRUD, lấy theo key/category  
**Views** — Server-side Thymeleaf pages cho tất cả màn hình

### 1.3 Chức năng đã hoàn thiện

- ✅ Authentication & Authorization (RBAC) với OAuth2
- ✅ CRUD đầy đủ: Customer, Order, Measurement, Payment
- ✅ Tìm kiếm, phân trang, soft delete
- ✅ Audit logging & API access logging
- ✅ Report cơ bản (doanh thu, thống kê)
- ✅ Cấu hình hệ thống (Settings)
- ✅ Data initialization (roles, admin user, measurement templates)
- ✅ UI với Bootstrap 5 + jQuery + Chart.js

---

## II. YÊU CẦU TỪ FILE EXCEL

### Các phân hệ chức năng yêu cầu

| STT | Phân hệ | Trạng thái |
|-----|---------|-----------|
| 1 | Quản lý Kênh Tiếp nhận (Omnichannel) | ❌ Chưa có |
| 2 | Quản lý Khách hàng / CRM | ⚠️ Có một phần |
| 3 | Quản lý Đơn hàng & Báo giá | ⚠️ Có một phần |
| 4 | Quản lý Sản xuất & Tiến độ | ❌ Chưa có |
| 5 | Kiểm soát Chất lượng (QC) & Giao hàng | ❌ Chưa có |
| 6 | Chăm sóc Sau Bán Hàng | ❌ Chưa có |
| 7 | Tài chính & Hoa hồng | ⚠️ Có một phần |
| 8 | Báo cáo & Đánh giá (Analytics) | ⚠️ Có một phần |

### Quy tắc nghiệp vụ đặc thù

- **Phân khách theo Performance Score:** Điểm số do quản lý định nghĩa, hệ thống tính tự động. Nhân viên điểm thấp cần manager phê duyệt thủ công.
- **Đặt cọc bắt buộc:** Phải xác nhận đặt cọc trước khi hệ thống cho phép đặt vải và bắt đầu sản xuất.
- **2 nhân viên per đơn hàng:** Ghi nhận người chính + người phụ, chia quyền lợi và hoa hồng.
- **Hoa hồng chỉnh sửa được:** Mỗi công đoạn sản xuất cho phép chỉnh sửa hoa hồng cho nhân viên.

---

## III. DANH SÁCH CÔNG VIỆC CẦN LÀM

---

### PHÂN HỆ 1 — Quản lý Kênh Tiếp nhận (Omnichannel)
> Hoàn toàn chưa có — cần xây dựng mới từ đầu

| # | Công việc | Độ ưu tiên |
|---|-----------|-----------|
| 1.1 | Tạo entity `Channel` (Messenger, Zalo, WhatsApp, Email) + migration SQL | 🔴 Cao |
| 1.2 | Tạo entity `Lead` — bản ghi tiếp nhận (SĐT, email, nội dung, trạng thái, kênh nguồn) | 🔴 Cao |
| 1.3 | Tạo `LeadController`, `LeadService`, `LeadRepository`, `LeadDTO` | 🔴 Cao |
| 1.4 | API phân loại lead tự động theo nhu cầu/nhóm, tự bắt SĐT/Email | 🔴 Cao |
| 1.5 | Màn hình quản lý tập trung tất cả kênh (HTML + JS) | 🔴 Cao |
| 1.6 | Tích hợp webhook từ Zalo/Messenger (hoặc form nhập tay từ kênh) | 🟡 Trung bình |
| 1.7 | Chatbot Q&A: lưu bộ câu hỏi thường gặp & gợi ý trả lời từ lịch sử | 🟢 Thấp |

---

### PHÂN HỆ 2 — Quản lý Khách hàng / CRM
> Một phần đã có — cần bổ sung thêm

| # | Công việc | Độ ưu tiên |
|---|-----------|-----------|
| 2.1 | Thêm field `assignedStaffId` vào entity `Customer` + migration | 🔴 Cao |
| 2.2 | Thêm field `cac` (chi phí có khách) & `interactionHistory` vào `Customer` | 🔴 Cao |
| 2.3 | Tạo entity `Staff` với `performanceScore`, `totalLeads`, `conversionRate` | 🔴 Cao |
| 2.4 | Logic **Lead Distribution**: phân khách xoay vòng hoặc theo `performanceScore` | 🔴 Cao |
| 2.5 | Logic nhận diện khách cũ: ưu tiên gán về nhân viên đã từng chăm sóc | 🔴 Cao |
| 2.6 | Cơ chế phê duyệt thủ công khi nhân viên điểm thấp (Manager approval workflow) | 🔴 Cao |
| 2.7 | UI CRM: hiển thị nhân viên phụ trách, lịch sử tương tác, CAC | 🟡 Trung bình |

---

### PHÂN HỆ 3 — Quản lý Đơn hàng & Báo giá
> Một phần đã có — cần mở rộng đáng kể

| # | Công việc | Độ ưu tiên |
|---|-----------|-----------|
| 3.1 | Thêm field vào `TailoringOrder`: `fabricMaterial`, `accessories`, `sourceChannel` | 🔴 Cao |
| 3.2 | Tạo entity `Quotation` (Báo giá) riêng, liên kết `Order` sau khi chốt | 🔴 Cao |
| 3.3 | Tạo entity `Affiliate` (Đối tác) với mã giảm giá, tỷ lệ hoa hồng | 🔴 Cao |
| 3.4 | Tạo entity `DiscountCode` liên kết `Affiliate` | 🔴 Cao |
| 3.5 | Logic áp mã giảm giá: giảm cho khách + hoa hồng cho đối tác | 🔴 Cao |
| 3.6 | Thêm field `depositAmount`, `depositStatus`, `depositDate` vào `TailoringOrder` | 🔴 Cao |
| 3.7 | Logic khóa luồng sản xuất: chỉ cho phép tiến khi đã xác nhận đặt cọc | 🔴 Cao |
| 3.8 | Tạo entity `StaffCommission`: hoa hồng cho nhân viên chính & phụ per order | 🔴 Cao |
| 3.9 | Logic 2 nhân viên / đơn hàng (người chính / người phụ) & chia hoa hồng | 🔴 Cao |
| 3.10 | UI trang báo giá: form nhập vải, phụ liệu, số lượng, đơn giá, nguồn khách | 🟡 Trung bình |

---

### PHÂN HỆ 4 — Quản lý Sản xuất & Tiến độ
> Hoàn toàn chưa có — ưu tiên cao nhất

| # | Công việc | Độ ưu tiên |
|---|-----------|-----------|
| 4.1 | Tạo entity `ProductionStage` với enum: `CUT → ASSEMBLE → FITTING → DELIVERY` | 🔴 Cao |
| 4.2 | Tạo entity `ProductionCalendar` — lịch sản xuất của từng thợ | 🔴 Cao |
| 4.3 | Thêm field `assignedWorker`, `assignedSale` vào từng `ProductionStage` | 🔴 Cao |
| 4.4 | Tạo bảng `production_stage_log` — lưu vết mọi thay đổi (thợ, lịch, hoa hồng, ghi chú) | 🔴 Cao |
| 4.5 | API CRUD `ProductionStage` + cập nhật trạng thái từng công đoạn | 🔴 Cao |
| 4.6 | Logic cảnh báo màu: **Xanh** (đúng hạn) / **Vàng** (sắp đến hạn) / **Đỏ** (quá hạn) — sale tự config ngưỡng | 🔴 Cao |
| 4.7 | Mỗi sale/thợ có **Calendar riêng** + 1 **Calendar tổng** cho đơn hàng | 🔴 Cao |
| 4.8 | API lấy calendar theo sale / theo thợ / theo đơn hàng | 🔴 Cao |
| 4.9 | UI trang Production: timeline 4 bước, màu cảnh báo, gán thợ, ghi chú | 🔴 Cao |
| 4.10 | UI Calendar view (FullCalendar.js) | 🟡 Trung bình |

---

### PHÂN HỆ 5 — Kiểm soát Chất lượng (QC) & Giao hàng
> Chưa có — cần xây dựng mới

| # | Công việc | Độ ưu tiên |
|---|-----------|-----------|
| 5.1 | Tạo entity `QCCheck` — checklist kiểm tra: chỉ thừa, phấn kẻ, vải | 🔴 Cao |
| 5.2 | API xác nhận QC pass/fail kèm ghi chú | 🔴 Cao |
| 5.3 | Logic tất toán & giao hàng: xác nhận thanh toán phần còn lại → order = `COMPLETED` | 🔴 Cao |
| 5.4 | UI trang QC & giao hàng | 🟡 Trung bình |

---

### PHÂN HỆ 6 — Chăm sóc Sau Bán Hàng
> Chưa có

| # | Công việc | Độ ưu tiên |
|---|-----------|-----------|
| 6.1 | Tạo entity `FollowUpReminder` — nhắc nhở sau 3-10 ngày kể từ ngày giao | 🟡 Trung bình |
| 6.2 | Scheduler tự động tạo reminder sau khi order hoàn thành | 🟡 Trung bình |
| 6.3 | UI danh sách follow-up cần thực hiện hôm nay | 🟡 Trung bình |
| 6.4 | Ghi nhận kết quả cuộc gọi/nhắn tin chăm sóc | 🟢 Thấp |

---

### PHÂN HỆ 7 — Tài chính & Hoa hồng
> Thanh toán cơ bản đã có — cần mở rộng

| # | Công việc | Độ ưu tiên |
|---|-----------|-----------|
| 7.1 | Tạo entity `Commission` — tính và lưu hoa hồng từng nhân viên per order/giai đoạn | 🔴 Cao |
| 7.2 | Logic tự động tính hoa hồng: nhân viên chính/phụ theo tỷ lệ quy định | 🔴 Cao |
| 7.3 | Cho phép chỉnh sửa hoa hồng thủ công từng công đoạn | 🔴 Cao |
| 7.4 | Tạo entity `Expense` — chi phí vận hành: mặt bằng, nguyên liệu, v.v. | 🟡 Trung bình |
| 7.5 | API báo cáo hoa hồng theo nhân viên / theo kỳ | 🟡 Trung bình |
| 7.6 | UI quản lý hoa hồng & chi phí | 🟡 Trung bình |

---

### PHÂN HỆ 8 — Báo cáo & Đánh giá (Analytics)
> Báo cáo cơ bản đã có — cần mở rộng

| # | Công việc | Độ ưu tiên |
|---|-----------|-----------|
| 8.1 | Báo cáo doanh thu theo kênh marketing (nguồn khách) | 🔴 Cao |
| 8.2 | Báo cáo doanh thu theo nhân viên | 🔴 Cao |
| 8.3 | Đánh giá nhân sự: `performanceScore`, năng suất, tỷ lệ lỗi/đúng hạn | 🔴 Cao |
| 8.4 | Tỷ lệ chuyển đổi lead → đơn hàng | 🟡 Trung bình |
| 8.5 | Báo cáo hiệu quả marketing | 🟡 Trung bình |
| 8.6 | Xuất báo cáo ra Excel/PDF | 🟢 Thấp |

---

### CÔNG VIỆC KỸ THUẬT / NỀN TẢNG

| # | Công việc | Độ ưu tiên |
|---|-----------|-----------|
| T.1 | Viết Flyway migration SQL cho tất cả bảng mới (V2, V3, V5, V6...) | 🔴 Cao |
| T.2 | Xây dựng entity `Staff` tách biệt hoặc extend `User` — quản lý nhân viên may | 🔴 Cao |
| T.3 | Bổ sung role `STAFF` (thợ may) và `SALE` vào hệ thống phân quyền | 🔴 Cao |
| T.4 | API quản lý Staff: CRUD, xem calendar, xem hoa hồng | 🔴 Cao |
| T.5 | Xây dựng notification system cơ bản (in-app notification) | 🟡 Trung bình |
| T.6 | Unit tests cho các service mới | 🟢 Thấp |

---

## IV. LỘ TRÌNH TRIỂN KHAI ĐỀ XUẤT

### Sprint 1 — Nền tảng nghiệp vụ cốt lõi
> Ưu tiên cao nhất, cần làm trước

- [ ] **T.1** Flyway migration SQL cho bảng mới
- [ ] **T.2** Entity `Staff` + roles `SALE`, `STAFF`
- [ ] **T.3** Bổ sung role mới vào Security
- [ ] **3.1** Mở rộng `TailoringOrder`: vải, phụ liệu, nguồn khách
- [ ] **3.6** Đặt cọc: `depositAmount`, `depositStatus`, `depositDate`
- [ ] **3.7** Logic khóa luồng sản xuất trước khi đặt cọc
- [ ] **3.8–3.9** Hoa hồng 2 nhân viên / đơn hàng
- [ ] **4.1–4.5** Quy trình sản xuất 4 bước + log thay đổi
- [ ] **4.6** Cảnh báo màu xanh/vàng/đỏ

### Sprint 2 — QC, Tài chính, CRM
- [ ] **5.1–5.3** QC checklist & tất toán giao hàng
- [ ] **7.1–7.3** Hoa hồng tự động + chỉnh sửa thủ công
- [ ] **7.4** Quản lý chi phí vận hành
- [ ] **2.3–2.6** Staff Performance Score + Lead Distribution
- [ ] **4.7–4.10** Calendar view (sale/thợ/đơn hàng)

### Sprint 3 — Analytics, Omnichannel, After-sales
- [ ] **8.1–8.5** Mở rộng báo cáo Analytics
- [ ] **1.1–1.5** Kênh tiếp nhận (Omnichannel)
- [ ] **6.1–6.3** Chăm sóc sau bán hàng
- [ ] **T.5** In-app notifications
- [ ] **8.6** Xuất Excel/PDF

---

## V. TỔNG KẾT

| Hạng mục | Con số |
|----------|--------|
| Tổng số công việc cần làm | ~55 tasks |
| Entities cần tạo mới | 10+ (Staff, Lead, Channel, Quotation, Affiliate, DiscountCode, ProductionStage, QCCheck, Commission, Expense, FollowUpReminder) |
| Migrations SQL cần viết | ~5 file (V2–V6) |
| HTML pages mới cần tạo | ~5 (production, qc, crm, commission, leads) |
| Chức năng hiện có / Yêu cầu | ~30% / 100% |

> **Ưu tiên triển khai ngay:** Phân hệ 3 (đặt cọc + hoa hồng) và Phân hệ 4 (quy trình sản xuất 4 bước) vì đây là nghiệp vụ cốt lõi và khác biệt lớn nhất so với hệ thống hiện tại.
