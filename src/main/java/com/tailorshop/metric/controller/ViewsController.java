package com.tailorshop.metric.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * View Controller for serving HTML pages
 */
@Controller
@RequestMapping("/pages")
public class ViewsController {

    /**
     * Serve login page
     */
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    /**
     * Serve signup page
     */
    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    /**
     * Serve dashboard page
     */
    @GetMapping("/dashboard")
    public String dashboardPage() {
        return "dashboard";
    }

    /**
     * Serve orders management page
     */
    @GetMapping("/orders")
    public String ordersPage() {
        return "orders";
    }

    /**
     * Serve measurements page
     */
    @GetMapping("/measurements")
    public String measurementsPage() {
        return "measurements";
    }

    /**
     * Serve customers management page
     */
    @GetMapping("/customers")
    public String customersPage() {
        return "customers";
    }

    /**
     * Serve payments management page
     */
    @GetMapping("/payments")
    public String paymentsPage() {
        return "payments";
    }

    /**
     * Serve reports page
     */
    @GetMapping("/reports")
    public String reportsPage() {
        return "reports";
    }

    /**
     * Serve settings page
     */
    @GetMapping("/settings")
    public String settingsPage() {
        return "settings";
    }

    /**
     * Serve leads / omnichannel management page
     */
    @GetMapping("/leads")
    public String leadsPage() {
        return "leads";
    }

    /**
     * Serve CRM / staff management page
     */
    @GetMapping("/crm")
    public String crmPage() {
        return "crm";
    }

    /**
     * Serve Quotation & Deposit management page (Phân hệ 3)
     */
    @GetMapping("/quotation")
    public String quotationPage() {
        return "quotation";
    }

    /**
     * Serve Production & Progress management page (Phân hệ 4)
     */
    @GetMapping("/production")
    public String productionPage() {
        return "production";
    }

    /**
     * Serve QC & Delivery management page (Phân hệ 5)
     */
    @GetMapping("/qc")
    public String qcPage() {
        return "qc";
    }

    /**
     * Serve After-Sales Care management page (Phân hệ 6)
     */
    @GetMapping("/aftersales")
    public String afterSalesPage() {
        return "aftersales";
    }

    /**
     * Serve Finance & Commission management page (Phân hệ 7)
     */
    @GetMapping("/finance")
    public String financePage() {
        return "finance";
    }

}
