/**
 * Customers Module
 * Handles customer management functionality and API calls
 */

class CustomerService {
    constructor() {
        this.apiBaseUrl = '/api/customers';
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
            headers,
            credentials: 'same-origin'
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
     * Get all customers with pagination
     */
    async getCustomers(page = 0, size = 10, search = '') {
        try {
            const url = `${this.apiBaseUrl}?page=${page}&size=${size}`;
            const response = await this.makeRequest(url);
            return response;
        } catch (error) {
            console.error('Error fetching customers:', error);
            throw error;
        }
    }

    /**
     * Search customers by name
     */
    async searchCustomers(query, page = 0, size = 10) {
        try {
            const url = `${this.apiBaseUrl}/search?q=${encodeURIComponent(query)}&page=${page}&size=${size}`;
            const response = await this.makeRequest(url);
            return response;
        } catch (error) {
            console.error('Error searching customers:', error);
            throw error;
        }
    }

    /**
     * Get customer by ID
     */
    async getCustomer(id) {
        try {
            const response = await this.makeRequest(`${this.apiBaseUrl}/${id}`);
            return response;
        } catch (error) {
            console.error('Error fetching customer:', error);
            throw error;
        }
    }

    /**
     * Create new customer
     */
    async createCustomer(customerData) {
        try {
            const response = await this.makeRequest(`${this.apiBaseUrl}`, {
                method: 'POST',
                body: JSON.stringify(customerData)
            });
            return response;
        } catch (error) {
            console.error('Error creating customer:', error);
            throw error;
        }
    }

    /**
     * Update customer
     */
    async updateCustomer(id, customerData) {
        try {
            const response = await this.makeRequest(`${this.apiBaseUrl}/${id}`, {
                method: 'PUT',
                body: JSON.stringify(customerData)
            });
            return response;
        } catch (error) {
            console.error('Error updating customer:', error);
            throw error;
        }
    }

    /**
     * Delete customer
     */
    async deleteCustomer(id) {
        try {
            const response = await this.makeRequest(`${this.apiBaseUrl}/${id}`, {
                method: 'DELETE'
            });
            return response;
        } catch (error) {
            console.error('Error deleting customer:', error);
            throw error;
        }
    }
}

const customerService = new CustomerService();

let currentPage = 0;
const pageSize = 10;
let customers = [];
let totalPages = 0;

/**
 * Initialize function for customers page
 */
function initCustomersPage() {
    loadCustomers();
    setupEventListeners();
}

/**
 * Initialize Page
 */
$(document).ready(function () {
    initCustomersPage();
});

/**
 * Support for dynamic page loading
 */
document.addEventListener('pageLoaded', function(event) {
    if (event.detail && event.detail.page === 'customers') {
        initCustomersPage();
    }
});

/**
 * Load customers from API
 */
async function loadCustomers(page = 0) {
    try {
        $('#customersContainer').html('<p class="text-center">Loading customers...</p>');
        
        const searchTerm = $('#searchInput').val();
        let response;
        
        if (searchTerm.trim()) {
            response = await customerService.searchCustomers(searchTerm, page, pageSize);
        } else {
            response = await customerService.getCustomers(page, pageSize);
        }
        
        if (response.success && response.data) {
            customers = response.data.content || [];
            totalPages = response.data.totalPages || 0;
            currentPage = page;
            
            displayCustomers();
            updatePagination();
        } else {
            showError('Failed to load customers');
        }
    } catch (error) {
        showError(error.message || 'Error loading customers');
        console.error('Load customers error:', error);
    }
}

/**
 * Display customers in cards
 */
function displayCustomers() {
    const container = $('#customersContainer');
    container.empty();

    if (customers.length === 0) {
        container.html('<p class="text-center text-muted">No customers found</p>');
        return;
    }

    customers.forEach(customer => {
        const card = `
            <div class="customer-card">
                <div class="row">
                    <div class="col-md-6">
                        <h6>${customer.firstName} ${customer.lastName}</h6>
                        <div class="customer-info">
                            <p><i class="fas fa-envelope"></i> ${customer.email}</p>
                            <p><i class="fas fa-phone"></i> ${customer.phone || 'N/A'}</p>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="customer-info">
                            ${customer.address ? `<p><i class="fas fa-map-marker-alt"></i> ${customer.address}</p>` : ''}
                            ${customer.city ? `<p>${customer.city}${customer.postalCode ? ', ' + customer.postalCode : ''}</p>` : ''}
                        </div>
                    </div>
                </div>
                <div class="mt-2">
                    <button class="btn btn-sm btn-outline-primary" onclick="editCustomer(${customer.id})">
                        <i class="fas fa-edit"></i> Edit
                    </button>
                    <button class="btn btn-sm btn-outline-danger" onclick="deleteCustomer(${customer.id})">
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
        li.innerHTML = `<a class="page-link" href="#" onclick="loadCustomers(${i}); return false;">${i + 1}</a>`;
        pageNumbers.append(li);
    }
}

/**
 * Next page
 */
function nextPage() {
    if (currentPage < totalPages - 1) {
        loadCustomers(currentPage + 1);
    }
}

/**
 * Previous page
 */
function previousPage() {
    if (currentPage > 0) {
        loadCustomers(currentPage - 1);
    }
}

/**
 * Edit customer
 */
async function editCustomer(id) {
    try {
        const response = await customerService.getCustomer(id);
        if (response.success && response.data) {
            const customer = response.data;
            $('#editCustomerId').val(customer.id);
            $('#editFirstName').val(customer.firstName);
            $('#editLastName').val(customer.lastName);
            $('#editEmail').val(customer.email);
            $('#editPhone').val(customer.phone);
            $('#editAddress').val(customer.address || '');
            $('#editCity').val(customer.city || '');
            $('#editPostalCode').val(customer.postalCode || '');
            $('#editNotes').val(customer.notes || '');
            
            new bootstrap.Modal(document.getElementById('editCustomerModal')).show();
        }
    } catch (error) {
        showError(error.message || 'Error loading customer');
    }
}

/**
 * Delete customer
 */
async function deleteCustomer(id) {
    if (confirm('Are you sure you want to delete this customer?')) {
        try {
            await customerService.deleteCustomer(id);
            showSuccess('Customer deleted successfully');
            loadCustomers(0);
        } catch (error) {
            showError(error.message || 'Error deleting customer');
        }
    }
}

/**
 * Setup event listeners
 */
function setupEventListeners() {
    // Create customer form submit
    $('#customerForm').on('submit', async function (e) {
        e.preventDefault();

        const valid = window.FormValidator && FormValidator.validate('createCustomerModal', [
            { id: 'firstName',  label: 'Họ',    required: true },
            { id: 'lastName',   label: 'Tên',   required: true },
            { id: 'email',      label: 'Email', required: true, type: 'email' },
            { id: 'phone',      label: 'Số điện thoại', required: true }
        ]);
        if (valid === false) return;

        const customerData = {
            firstName: $('#firstName').val(),
            lastName: $('#lastName').val(),
            email: $('#email').val(),
            phone: $('#phone').val(),
            address: $('#address').val(),
            city: $('#city').val(),
            postalCode: $('#postalCode').val(),
            notes: $('#notes').val()
        };

        try {
            await customerService.createCustomer(customerData);
            showSuccess('Customer created successfully');
            this.reset();
            bootstrap.Modal.getInstance(document.getElementById('createCustomerModal')).hide();
            loadCustomers(0);
        } catch (error) {
            showError(error.message || 'Error creating customer');
        }
    });

    // Edit customer form submit
    $('#editCustomerForm').on('submit', async function (e) {
        e.preventDefault();

        const valid = window.FormValidator && FormValidator.validate('editCustomerModal', [
            { id: 'editFirstName', label: 'Họ',    required: true },
            { id: 'editLastName',  label: 'Tên',   required: true },
            { id: 'editEmail',     label: 'Email', required: true, type: 'email' },
            { id: 'editPhone',     label: 'Số điện thoại', required: true }
        ]);
        if (valid === false) return;

        const customerId = $('#editCustomerId').val();
        const customerData = {
            firstName: $('#editFirstName').val(),
            lastName: $('#editLastName').val(),
            email: $('#editEmail').val(),
            phone: $('#editPhone').val(),
            address: $('#editAddress').val(),
            city: $('#editCity').val(),
            postalCode: $('#editPostalCode').val(),
            notes: $('#editNotes').val()
        };

        try {
            await customerService.updateCustomer(customerId, customerData);
            showSuccess('Customer updated successfully');
            bootstrap.Modal.getInstance(document.getElementById('editCustomerModal')).hide();
            loadCustomers(currentPage);
        } catch (error) {
            showError(error.message || 'Error updating customer');
        }
    });

    // Search input with debounce
    let searchTimeout;
    $('#searchInput').on('keyup', function () {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => {
            loadCustomers(0);
        }, 300);
    });
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
