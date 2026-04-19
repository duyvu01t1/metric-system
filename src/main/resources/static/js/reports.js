/**
 * Reports Module
 * Handles report generation and visualization
 */

class ReportService {
    constructor() {
        this.apiBaseUrl = '/api/reports';
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
     * Get revenue report
     */
    async getRevenueReport(startDate, endDate) {
        try {
            let url = `${this.apiBaseUrl}/revenue`;
            if (startDate) url += `?startDate=${startDate}`;
            if (endDate) url += `${startDate ? '&' : '?'}endDate=${endDate}`;
            
            const response = await this.makeRequest(url);
            return response;
        } catch (error) {
            console.error('Error fetching revenue report:', error);
            throw error;
        }
    }

    /**
     * Get customer report
     */
    async getCustomerReport() {
        try {
            const response = await this.makeRequest(`${this.apiBaseUrl}/customers`);
            return response;
        } catch (error) {
            console.error('Error fetching customer report:', error);
            throw error;
        }
    }

    /**
     * Get orders report
     */
    async getOrdersReport() {
        try {
            const response = await this.makeRequest(`${this.apiBaseUrl}/orders`);
            return response;
        } catch (error) {
            console.error('Error fetching orders report:', error);
            throw error;
        }
    }

    /**
     * Get dashboard summary
     */
    async getDashboardSummary() {
        try {
            const response = await this.makeRequest(`${this.apiBaseUrl}/dashboard-summary`);
            return response;
        } catch (error) {
            console.error('Error fetching dashboard summary:', error);
            throw error;
        }
    }
}

const reportService = new ReportService();

// Chart instances
let orderStatusChart = null;
let revenueChart = null;

/**
 * Initialize function for reports page
 */
function initReportsPage() {
    setDefaultDates();
    loadAllReports();
}

/**
 * Initialize Page
 */
$(document).ready(function () {
    initReportsPage();
});

/**
 * Support for dynamic page loading
 */
document.addEventListener('pageLoaded', function(event) {
    if (event.detail && event.detail.page === 'reports') {
        initReportsPage();
    }
});

/**
 * Set default dates (last month)
 */
function setDefaultDates() {
    const today = new Date();
    const lastMonth = new Date(today.getFullYear(), today.getMonth() - 1, today.getDate());
    
    $('#revenueStartDate').val(formatDate(lastMonth));
    $('#revenueEndDate').val(formatDate(today));
}

/**
 * Load all reports
 */
async function loadAllReports() {
    try {
        await loadRevenueReport();
        await loadCustomerReport();
        await loadOrdersReport();
        initializeCharts();
    } catch (error) {
        showError(error.message || 'Error loading reports');
    }
}

/**
 * Load revenue report
 */
async function loadRevenueReport() {
    try {
        const startDate = $('#revenueStartDate').val();
        const endDate = $('#revenueEndDate').val();
        
        const response = await reportService.getRevenueReport(startDate, endDate);
        
        if (response.success && response.data) {
            const data = response.data;
            
            $('#totalRevenue').text('$' + (parseFloat(data.totalRevenue) || 0).toFixed(2));
            $('#totalPayments').text('$' + (parseFloat(data.totalPayments) || 0).toFixed(2));
            $('#totalOrders').text(data.totalOrders || 0);
            $('#completedOrders').text(data.completedOrders || 0);
            
            updateRevenueChart(data);
        }
    } catch (error) {
        showError(error.message || 'Error loading revenue report');
    }
}

/**
 * Load customer report
 */
async function loadCustomerReport() {
    try {
        const response = await reportService.getCustomerReport();
        
        if (response.success && response.data) {
            const data = response.data;
            
            $('#totalCustomers').text(data.totalCustomers || 0);
            $('#activeCustomers').text(data.activeCustomers || 0);
        }
    } catch (error) {
        showError(error.message || 'Error loading customer report');
    }
}

/**
 * Load orders report
 */
async function loadOrdersReport() {
    try {
        const response = await reportService.getOrdersReport();
        
        if (response.success && response.data) {
            const data = response.data;
            
            $('#pendingOrders').text(data.pendingOrders || 0);
            $('#completionRate').text((data.completionRate || 0).toFixed(1) + '%');
            
            updateOrderStatusChart(data);
        }
    } catch (error) {
        showError(error.message || 'Error loading orders report');
    }
}

/**
 * Initialize charts
 */
function initializeCharts() {
    // Order Status Chart
    const orderCtx = document.getElementById('orderStatusChart');
    if (orderCtx && !orderStatusChart) {
        orderStatusChart = new Chart(orderCtx, {
            type: 'doughnut',
            data: {
                labels: ['Completed', 'Pending', 'Other'],
                datasets: [{
                    data: [0, 0, 0],
                    backgroundColor: [
                        '#28a745',
                        '#ffc107',
                        '#dc3545'
                    ]
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom'
                    }
                }
            }
        });
    }

    // Revenue Chart
    const revenueCtx = document.getElementById('revenueChart');
    if (revenueCtx && !revenueChart) {
        revenueChart = new Chart(revenueCtx, {
            type: 'bar',
            data: {
                labels: ['Total Revenue', 'Total Payments'],
                datasets: [{
                    label: 'Amount ($)',
                    data: [0, 0],
                    backgroundColor: [
                        '#1976d2',
                        '#f57c00'
                    ]
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                indexAxis: 'y',
                plugins: {
                    legend: {
                        display: false
                    }
                },
                scales: {
                    x: {
                        beginAtZero: true
                    }
                }
            }
        });
    }
}

/**
 * Update order status chart
 */
function updateOrderStatusChart(data) {
    if (orderStatusChart) {
        const total = data.totalOrders || 0;
        const completed = data.completedOrders || 0;
        const pending = data.pendingOrders || 0;
        const other = total - completed - pending;
        
        orderStatusChart.data.datasets[0].data = [completed, pending, Math.max(0, other)];
        orderStatusChart.update();
    }
}

/**
 * Update revenue chart
 */
function updateRevenueChart(data) {
    if (revenueChart) {
        const revenue = parseFloat(data.totalRevenue) || 0;
        const payments = parseFloat(data.totalPayments) || 0;
        
        revenueChart.data.datasets[0].data = [revenue, payments];
        revenueChart.update();
    }
}

/**
 * Format date to yyyy-MM-dd
 */
function formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

/**
 * Export to CSV
 */
function exportToCSV() {
    const csvContent = "data:text/csv;charset=utf-8," + 
        "Report Type,Metric,Value\n" +
        "Revenue,Total Revenue," + $('#totalRevenue').text() + "\n" +
        "Revenue,Total Payments," + $('#totalPayments').text() + "\n" +
        "Orders,Total Orders," + $('#totalOrders').text() + "\n" +
        "Orders,Completed Orders," + $('#completedOrders').text() + "\n" +
        "Orders,Pending Orders," + $('#pendingOrders').text() + "\n" +
        "Customers,Total Customers," + $('#totalCustomers').text() + "\n" +
        "Customers,Active Customers," + $('#activeCustomers').text();

    const link = document.createElement("a");
    link.setAttribute("href", encodeURI(csvContent));
    link.setAttribute("download", "report_" + new Date().toISOString().split('T')[0] + ".csv");
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    showSuccess('Report exported successfully');
}

/**
 * Print report
 */
function printReport() {
    window.print();
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
    const successAlert = $('<div class="alert alert-success alert-dismissible fade show" role="alert">')
        .html(message + '<button type="button" class="btn-close" data-bs-dismiss="alert"></button>');
    
    $('body').prepend(successAlert);
    window.scrollTo(0, 0);
    
    setTimeout(() => {
        successAlert.remove();
    }, 5000);
}
