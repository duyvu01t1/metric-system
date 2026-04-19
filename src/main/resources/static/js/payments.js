/**
 * Payments Module
 * Handles payment management functionality and API calls
 */

class PaymentService {
    constructor() {
        this.apiBaseUrl = '/api/payments';
    }

    /**
     * Make authenticated API request
     */
    async makeRequest(url, options = {}) {
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };

        const token = localStorage.getItem('auth_token');
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const response = await fetch(url, {
            ...options,
            headers
        });

        if (!response.ok) {
            if (response.status === 401) {
                localStorage.clear();
                window.location.href = '/api/pages/login';
            }
            const error = new Error(`HTTP error! status: ${response.status}`);
            error.status = response.status;
            throw error;
        }

        return await response.json();
    }

    /**
     * Get all payments with pagination
     */
    async getPayments(page = 0, size = 10) {
        try {
            const url = `${this.apiBaseUrl}?page=${page}&size=${size}`;
            const response = await this.makeRequest(url);
            return response;
        } catch (error) {
            console.error('Error fetching payments:', error);
            throw error;
        }
    }

    /**
     * Get payment by ID
     */
    async getPayment(id) {
        try {
            const response = await this.makeRequest(`${this.apiBaseUrl}/${id}`);
            return response;
        } catch (error) {
            console.error('Error fetching payment:', error);
            throw error;
        }
    }

    /**
     * Get payments by order ID
     */
    async getPaymentsByOrder(orderId) {
        try {
            const response = await this.makeRequest(`${this.apiBaseUrl}/order/${orderId}`);
            return response;
        } catch (error) {
            console.error('Error fetching payments by order:', error);
            throw error;
        }
    }

    /**
     * Create new payment
     */
    async createPayment(paymentData) {
        try {
            const response = await this.makeRequest(`${this.apiBaseUrl}`, {
                method: 'POST',
                body: JSON.stringify(paymentData)
            });
            return response;
        } catch (error) {
            console.error('Error creating payment:', error);
            throw error;
        }
    }

    /**
     * Update payment
     */
    async updatePayment(id, paymentData) {
        try {
            const response = await this.makeRequest(`${this.apiBaseUrl}/${id}`, {
                method: 'PUT',
                body: JSON.stringify(paymentData)
            });
            return response;
        } catch (error) {
            console.error('Error updating payment:', error);
            throw error;
        }
    }

    /**
     * Delete payment
     */
    async deletePayment(id) {
        try {
            const response = await this.makeRequest(`${this.apiBaseUrl}/${id}`, {
                method: 'DELETE'
            });
            return response;
        } catch (error) {
            console.error('Error deleting payment:', error);
            throw error;
        }
    }
}

class OrderService {
    constructor() {
        this.apiBaseUrl = '/api/orders';
    }

    /**
     * Make authenticated API request
     */
    async makeRequest(url, options = {}) {
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };

        const token = localStorage.getItem('auth_token');
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const response = await fetch(url, {
            ...options,
            headers
        });

        if (!response.ok) {
            if (response.status === 401) {
                localStorage.clear();
                window.location.href = '/api/pages/login';
            }
            const error = new Error(`HTTP error! status: ${response.status}`);
            error.status = response.status;
            throw error;
        }

        return await response.json();
    }

    /**
     * Get all orders for dropdown
     */
    async getAllOrders() {
        try {
            const response = await this.makeRequest(`${this.apiBaseUrl}?page=0&size=1000`);
            return response;
        } catch (error) {
            console.error('Error fetching orders:', error);
            return { data: { content: [] } };
        }
    }
}

const paymentService = new PaymentService();
const orderService = new OrderService();

let currentPage = 0;
const pageSize = 10;
let payments = [];
let totalPages = 0;

/**
 * Initialize function for payments page
 */
function initPaymentsPage() {
    loadPayments();
    loadOrders();
    setupEventListeners();
}

/**
 * Initialize Page
 */
$(document).ready(function () {
    initPaymentsPage();
});

/**
 * Support for dynamic page loading
 */
document.addEventListener('pageLoaded', function(event) {
    if (event.detail && event.detail.page === 'payments') {
        initPaymentsPage();
    }
});

/**
 * Load orders for dropdown
 */
async function loadOrders() {
    try {
        const response = await orderService.getAllOrders();
        if (response.success && response.data && response.data.content) {
            const orders = response.data.content;
            const select = $('#tailoringOrderId');
            select.empty();
            select.append('<option value="">Select Order</option>');
            
            orders.forEach(order => {
                select.append(`<option value="${order.id}">${order.orderCode} - ${order.customerName}</option>`);
            });
        }
    } catch (error) {
        console.error('Error loading orders:', error);
    }
}

/**
 * Load payments from API
 */
async function loadPayments(page = 0) {
    try {
        $('#paymentsContainer').html('<p class="text-center">Loading payments...</p>');
        
        const response = await paymentService.getPayments(page, pageSize);
        
        if (response.success && response.data) {
            payments = response.data.content || [];
            totalPages = response.data.totalPages || 0;
            currentPage = page;
            
            displayPayments();
            updatePagination();
        } else {
            showError('Failed to load payments');
        }
    } catch (error) {
        showError(error.message || 'Error loading payments');
        console.error('Load payments error:', error);
    }
}

/**
 * Display payments in cards
 */
function displayPayments() {
    const container = $('#paymentsContainer');
    container.empty();

    if (payments.length === 0) {
        container.html('<p class="text-center text-muted">No payments found</p>');
        return;
    }

    payments.forEach(payment => {
        const paymentDate = new Date(payment.paymentDate).toLocaleDateString();
        const methodBadge = `<span class="method-badge">${payment.paymentMethod || 'N/A'}</span>`;
        
        const card = `
            <div class="payment-card">
                <div class="row">
                    <div class="col-md-8">
                        <h6>${payment.orderCode}</h6>
                        <div class="payment-info">
                            <p><i class="fas fa-user"></i> ${payment.customerName || 'N/A'}</p>
                            <p><i class="fas fa-calendar"></i> ${paymentDate}</p>
                            <p>${methodBadge}</p>
                            ${payment.transactionReference ? `<p><i class="fas fa-hashtag"></i> ${payment.transactionReference}</p>` : ''}
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="text-end">
                            <div class="amount">$${parseFloat(payment.amount).toFixed(2)}</div>
                            ${payment.notes ? `<small class="d-block mt-2">${payment.notes}</small>` : ''}
                        </div>
                    </div>
                </div>
                <div class="mt-2">
                    <button class="btn btn-sm btn-outline-primary" onclick="editPayment(${payment.id})">
                        <i class="fas fa-edit"></i> Edit
                    </button>
                    <button class="btn btn-sm btn-outline-danger" onclick="deletePayment(${payment.id})">
                        <i class="fas fa-trash"></i> Delete
                    </button>
                </div>
            </div>
        `;
        container.append(card);
    });
}

/**
 * Update pagination
 */
function updatePagination() {
    const pageNumbers = $('#pageNumbers');
    pageNumbers.empty();

    for (let i = 0; i < totalPages; i++) {
        const li = document.createElement('li');
        li.className = `page-item ${i === currentPage ? 'active' : ''}`;
        li.innerHTML = `<a class="page-link" href="#" onclick="loadPayments(${i}); return false;">${i + 1}</a>`;
        pageNumbers.append(li);
    }
}

/**
 * Next page
 */
function nextPage() {
    if (currentPage < totalPages - 1) {
        loadPayments(currentPage + 1);
    }
}

/**
 * Previous page
 */
function previousPage() {
    if (currentPage > 0) {
        loadPayments(currentPage - 1);
    }
}

/**
 * Edit payment
 */
async function editPayment(id) {
    try {
        const response = await paymentService.getPayment(id);
        if (response.success && response.data) {
            const payment = response.data;
            $('#editPaymentId').val(payment.id);
            $('#editAmount').val(payment.amount);
            $('#editPaymentMethod').val(payment.paymentMethod);
            $('#editTransactionReference').val(payment.transactionReference || '');
            $('#editPaymentDate').val(formatDateTimeLocal(payment.paymentDate));
            $('#editNotes').val(payment.notes || '');
            
            new bootstrap.Modal(document.getElementById('editPaymentModal')).show();
        }
    } catch (error) {
        showError(error.message || 'Error loading payment');
    }
}

/**
 * Delete payment
 */
async function deletePayment(id) {
    if (confirm('Are you sure you want to delete this payment?')) {
        try {
            await paymentService.deletePayment(id);
            showSuccess('Payment deleted successfully');
            loadPayments(0);
        } catch (error) {
            showError(error.message || 'Error deleting payment');
        }
    }
}

/**
 * Setup event listeners
 */
function setupEventListeners() {
    // Create payment form submit
    $('#paymentForm').on('submit', async function (e) {
        e.preventDefault();

        const valid = window.FormValidator && FormValidator.validate('createPaymentModal', [
            { id: 'tailoringOrderId', label: 'Đơn hàng',          required: true },
            { id: 'amount',           label: 'Số tiền',            required: true, type: 'number', min: 0.01 },
            { id: 'paymentMethod',    label: 'Phương thức',       required: true },
            { id: 'paymentDate',      label: 'Ngày thanh toán',    required: true }
        ]);
        if (valid === false) return;

        const paymentData = {
            tailoringOrderId: $('#tailoringOrderId').val(),
            amount: $('#amount').val(),
            paymentMethod: $('#paymentMethod').val(),
            transactionReference: $('#transactionReference').val(),
            paymentDate: $('#paymentDate').val(),
            notes: $('#notes').val()
        };

        try {
            await paymentService.createPayment(paymentData);
            showSuccess('Payment recorded successfully');
            this.reset();
            bootstrap.Modal.getInstance(document.getElementById('createPaymentModal')).hide();
            loadPayments(0);
        } catch (error) {
            showError(error.message || 'Error recording payment');
        }
    });

    // Edit payment form submit
    $('#editPaymentForm').on('submit', async function (e) {
        e.preventDefault();

        const valid = window.FormValidator && FormValidator.validate('editPaymentModal', [
            { id: 'editAmount',        label: 'Số tiền',         required: true, type: 'number', min: 0.01 },
            { id: 'editPaymentMethod', label: 'Phương thức',    required: true },
            { id: 'editPaymentDate',   label: 'Ngày thanh toán', required: true }
        ]);
        if (valid === false) return;

        const paymentId = $('#editPaymentId').val();
        const paymentData = {
            amount: $('#editAmount').val(),
            paymentMethod: $('#editPaymentMethod').val(),
            transactionReference: $('#editTransactionReference').val(),
            paymentDate: $('#editPaymentDate').val(),
            notes: $('#editNotes').val()
        };

        try {
            await paymentService.updatePayment(paymentId, paymentData);
            showSuccess('Payment updated successfully');
            bootstrap.Modal.getInstance(document.getElementById('editPaymentModal')).hide();
            loadPayments(currentPage);
        } catch (error) {
            showError(error.message || 'Error updating payment');
        }
    });
}

/**
 * Format datetime to local string
 */
function formatDateTimeLocal(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
}

/**
 * Show error message
 */
function showError(message) {
    const errorAlert = $('#errorAlert');
    const errorMessage = $('#errorMessage');
    errorMessage.text(message);
    errorAlert.removeClass('hide').show();
    window.scrollTo(0, 0);
    setTimeout(() => {
        errorAlert.addClass('hide');
    }, 5000);
}

/**
 * Show success message
 */
function showSuccess(message) {
    const successAlert = $('#successAlert');
    const successMessage = $('#successMessage');
    successMessage.text(message);
    successAlert.removeClass('hide').show();
    window.scrollTo(0, 0);
    setTimeout(() => {
        successAlert.addClass('hide');
    }, 5000);
}
