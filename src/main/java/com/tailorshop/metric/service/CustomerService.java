package com.tailorshop.metric.service;

import com.tailorshop.metric.dto.CustomerDTO;
import com.tailorshop.metric.entity.Customer;
import com.tailorshop.metric.exception.BusinessException;
import com.tailorshop.metric.exception.ResourceNotFoundException;
import com.tailorshop.metric.repository.ChannelRepository;
import com.tailorshop.metric.repository.CustomerRepository;
import com.tailorshop.metric.repository.StaffRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Customer Service
 * Handles business logic for customer management
 */
@Service
@Slf4j
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private ChannelRepository channelRepository;

    /**
     * Create a new customer
     * @param customerDTO Customer data
     * @return Created customer
     */
    @Transactional
    public CustomerDTO createCustomer(CustomerDTO customerDTO) {
        log.info("Creating new customer: {}", customerDTO.getFirstName());
        
        Customer customer = convertToEntity(customerDTO);
        customer.setIsActive(true);
        customer.setCreatedAt(LocalDateTime.now());
        
        Customer saved = customerRepository.save(customer);
        log.info("Customer created with ID: {}", saved.getId());
        
        return convertToDTO(saved);
    }

    /**
     * Get customer by ID
     * @param id Customer ID
     * @return Customer data
     */
    @Transactional(readOnly = true)
    public CustomerDTO getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        return convertToDTO(customer);
    }

    /**
     * Get customer by code
     * @param customerCode Customer code
     * @return Customer data
     */
    @Transactional(readOnly = true)
    public CustomerDTO getCustomerByCode(String customerCode) {
        Customer customer = customerRepository.findByCustomerCode(customerCode)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found with code: " + customerCode));
        return convertToDTO(customer);
    }

    /**
     * Get all active customers
     * @param pageable Pagination info
     * @return Page of customers
     */
    @Transactional(readOnly = true)
    public Page<CustomerDTO> getAllActiveCustomers(Pageable pageable) {
        return customerRepository.findByIsActiveTrue(pageable)
            .map(this::convertToDTO);
    }

    /**
     * Search customers by name
     * @param searchTerm First or last name search term
     * @param pageable Pagination info
     * @return Page of matching customers
     */
    @Transactional(readOnly = true)
    public Page<CustomerDTO> searchCustomers(String searchTerm, Pageable pageable) {
        return customerRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            searchTerm, searchTerm, pageable
        ).map(this::convertToDTO);
    }

    /**
     * Update customer
     * @param id Customer ID
     * @param customerDTO Updated customer data
     * @return Updated customer
     */
    @Transactional
    public CustomerDTO updateCustomer(Long id, CustomerDTO customerDTO) {
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        
        customer.setFirstName(customerDTO.getFirstName());
        customer.setLastName(customerDTO.getLastName());
        customer.setEmail(customerDTO.getEmail());
        customer.setPhone(customerDTO.getPhone());
        customer.setAddress(customerDTO.getAddress());
        customer.setCity(customerDTO.getCity());
        customer.setPostalCode(customerDTO.getPostalCode());
        customer.setCountry(customerDTO.getCountry());
        customer.setIdentificationNumber(customerDTO.getIdentificationNumber());
        customer.setIdentificationType(customerDTO.getIdentificationType());
        customer.setDateOfBirth(customerDTO.getDateOfBirth());
        customer.setGender(customerDTO.getGender());
        customer.setNotes(customerDTO.getNotes());
        customer.setUpdatedAt(LocalDateTime.now());
        
        Customer updated = customerRepository.save(customer);
        log.info("Customer updated with ID: {}", id);
        
        return convertToDTO(updated);
    }

    /**
     * Deactivate customer
     * @param id Customer ID
     */
    @Transactional
    public void deactivateCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        
        customer.setIsActive(false);
        customer.setUpdatedAt(LocalDateTime.now());
        customerRepository.save(customer);
        log.info("Customer deactivated with ID: {}", id);
    }

    /**
     * Gán nhân viên phụ trách khách hàng (2.1)
     */
    @Transactional
    public CustomerDTO assignStaff(Long customerId, Long staffId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
        if (staffId != null) {
            staffRepository.findById(staffId)
                .orElseThrow(() -> new BusinessException("STAFF_NOT_FOUND", "Staff not found: " + staffId));
        }
        customer.setAssignedStaffId(staffId);
        return convertToDTO(customerRepository.save(customer));
    }

    /**
     * Convert Entity to DTO
     */
    private CustomerDTO convertToDTO(Customer customer) {
        CustomerDTO dto = CustomerDTO.builder()
            .id(customer.getId())
            .customerCode(customer.getCustomerCode())
            .firstName(customer.getFirstName())
            .lastName(customer.getLastName())
            .email(customer.getEmail())
            .phone(customer.getPhone())
            .address(customer.getAddress())
            .city(customer.getCity())
            .postalCode(customer.getPostalCode())
            .country(customer.getCountry())
            .identificationNumber(customer.getIdentificationNumber())
            .identificationType(customer.getIdentificationType())
            .dateOfBirth(customer.getDateOfBirth())
            .gender(customer.getGender())
            .notes(customer.getNotes())
            .isActive(customer.getIsActive())
            .assignedStaffId(customer.getAssignedStaffId())
            .cac(customer.getCac())
            .interactionCount(customer.getInteractionCount())
            .sourceChannelId(customer.getSourceChannelId())
            .lastInteractionAt(customer.getLastInteractionAt())
            .createdAt(customer.getCreatedAt())
            .updatedAt(customer.getUpdatedAt())
            .build();
        if (customer.getAssignedStaffId() != null) {
            staffRepository.findById(customer.getAssignedStaffId())
                .ifPresent(s -> dto.setAssignedStaffName(s.getFullName()));
        }
        if (customer.getSourceChannelId() != null) {
            channelRepository.findById(customer.getSourceChannelId())
                .ifPresent(c -> dto.setSourceChannelName(c.getDisplayName()));
        }
        return dto;
    }

    /**
     * Convert DTO to Entity
     */
    private Customer convertToEntity(CustomerDTO customerDTO) {
        Customer customer = new Customer();
        customer.setFirstName(customerDTO.getFirstName());
        customer.setLastName(customerDTO.getLastName());
        customer.setEmail(customerDTO.getEmail());
        customer.setPhone(customerDTO.getPhone());
        customer.setAddress(customerDTO.getAddress());
        customer.setCity(customerDTO.getCity());
        customer.setPostalCode(customerDTO.getPostalCode());
        customer.setCountry(customerDTO.getCountry());
        customer.setIdentificationNumber(customerDTO.getIdentificationNumber());
        customer.setIdentificationType(customerDTO.getIdentificationType());
        customer.setDateOfBirth(customerDTO.getDateOfBirth());
        customer.setGender(customerDTO.getGender());
        customer.setNotes(customerDTO.getNotes());
        return customer;
    }
}
