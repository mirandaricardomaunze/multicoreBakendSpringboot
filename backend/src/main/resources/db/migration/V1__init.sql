-- =====================================================================
-- V1__init.sql — Baseline do schema Multicore ERP (PostgreSQL).
--
-- Gerado a partir das entidades JPA (Hibernate, dialecto PostgreSQL) — ver
-- README.md nesta pasta. Representa o schema completo actual; as migrações
-- seguintes (V2, V3) são upgrades incrementais idempotentes (IF [NOT] EXISTS)
-- que assentam sobre este baseline sem conflito.
--
-- NÃO editar após aplicado em produção — criar uma nova migração V<n>.
-- =====================================================================

    create table absences (
        end_date date not null,
        has_supporting_document boolean not null,
        start_date date not null,
        total_days integer not null,
        created_at timestamp(6) not null,
        employee_id bigint not null,
        id bigserial not null,
        updated_at timestamp(6),
        absence_type varchar(30) not null,
        reason varchar(500),
        created_by varchar(255),
        primary key (id)
    );

    create table app_users (
        active boolean,
        created_at timestamp(6) not null,
        id bigserial not null,
        updated_at timestamp(6),
        created_by varchar(255),
        name varchar(255) not null,
        password varchar(255) not null,
        role varchar(255) not null,
        username varchar(255) not null unique,
        primary key (id)
    );

    create table approval_history (
        approval_request_id bigint not null,
        id bigserial not null,
        performed_at timestamp(6) not null,
        comments varchar(500),
        action varchar(255) not null,
        performed_by varchar(255) not null,
        role varchar(255) not null,
        primary key (id)
    );

    create table approval_requests (
        amount numeric(38,2) not null,
        created_at timestamp(6) not null,
        document_id bigint not null,
        id bigserial not null,
        updated_at timestamp(6),
        description varchar(500),
        rejection_reason varchar(500),
        created_by varchar(255),
        document_type varchar(255) not null,
        required_role varchar(255) not null,
        status varchar(255) not null check (status in ('PENDING','APPROVED','REJECTED')),
        submitter varchar(255) not null,
        primary key (id)
    );

    create table audit_logs (
        company_id bigint,
        event_time timestamp(6) not null,
        id bigserial not null,
        details varchar(1000),
        action varchar(255) not null,
        username varchar(255) not null,
        primary key (id)
    );

    create table clients (
        created_at timestamp(6) not null,
        id bigserial not null,
        updated_at timestamp(6),
        address varchar(255),
        created_by varchar(255),
        email varchar(255) not null,
        name varchar(255) not null,
        tax_id varchar(255) not null unique,
        primary key (id)
    );

    create table companies (
        created_at timestamp(6) not null,
        id bigserial not null,
        updated_at timestamp(6),
        address varchar(255),
        created_by varchar(255),
        email varchar(255),
        name varchar(255) not null,
        tax_id varchar(255) not null unique,
        primary key (id)
    );

    create table credit_note_lines (
        line_total numeric(14,2) not null,
        quantity numeric(12,3) not null,
        tax_rate numeric(5,4) not null,
        unit_price numeric(14,2) not null,
        credit_note_id bigint not null,
        id bigserial not null,
        invoice_line_id bigint,
        product_id bigint not null,
        batch_number varchar(255),
        primary key (id)
    );

    create table credit_notes (
        tax_amount numeric(14,2) not null,
        total_amount numeric(14,2) not null,
        total_before_tax numeric(14,2) not null,
        approved_at timestamp(6),
        client_id bigint not null,
        company_id bigint not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        invoice_id bigint not null,
        issue_date timestamp(6) not null,
        updated_at timestamp(6),
        warehouse_id bigint,
        reason varchar(20) not null check (reason in ('RETURN','DISCOUNT','ERROR','CANCELLATION')),
        status varchar(20) not null check (status in ('DRAFT','PENDING_APPROVAL','APPROVED','REJECTED','CANCELLED')),
        description varchar(500),
        rejection_reason varchar(500),
        approved_by varchar(255),
        created_by varchar(255),
        note_number varchar(255) not null unique,
        primary key (id)
    );

    create table crm_tickets (
        client_id bigint not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        updated_at timestamp(6),
        description varchar(1000) not null,
        created_by varchar(255),
        status varchar(255) not null,
        subject varchar(255) not null,
        primary key (id)
    );

    create table crm_work_sheets (
        hours_worked numeric(38,2) not null,
        is_billed boolean not null,
        parts_cost numeric(38,2) not null,
        total_value numeric(38,2) not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        ticket_id bigint not null,
        updated_at timestamp(6),
        parts_used varchar(500),
        description varchar(1000) not null,
        created_by varchar(255),
        technician_name varchar(255) not null,
        primary key (id)
    );

    create table customer_order_lines (
        discount_percentage numeric(5,2),
        line_total numeric(38,2) not null,
        quantity integer not null,
        tax_rate numeric(38,2) not null,
        unit_price numeric(38,2) not null,
        id bigserial not null,
        order_id bigint not null,
        product_id bigint not null,
        batch_number varchar(255),
        serial_number varchar(255),
        primary key (id)
    );

    create table customer_orders (
        print_count integer not null,
        tax_amount numeric(38,2) not null,
        total_amount numeric(38,2) not null,
        total_before_tax numeric(38,2) not null,
        client_id bigint not null,
        company_id bigint,
        created_at timestamp(6) not null,
        id bigserial not null,
        invoice_id bigint,
        printed_at timestamp(6),
        updated_at timestamp(6),
        warehouse_id bigint,
        last_printed_by varchar(80),
        walk_in_name varchar(120),
        created_by varchar(255),
        order_number varchar(255) unique,
        status varchar(255) not null,
        primary key (id)
    );

    create table debit_note_lines (
        amount numeric(14,2) not null,
        line_total numeric(14,2) not null,
        tax_rate numeric(5,4) not null,
        debit_note_id bigint not null,
        id bigserial not null,
        description varchar(300) not null,
        primary key (id)
    );

    create table debit_notes (
        tax_amount numeric(14,2) not null,
        total_amount numeric(14,2) not null,
        total_before_tax numeric(14,2) not null,
        approved_at timestamp(6),
        client_id bigint not null,
        company_id bigint not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        invoice_id bigint not null,
        issue_date timestamp(6) not null,
        updated_at timestamp(6),
        reason varchar(20) not null check (reason in ('FREIGHT','SURCHARGE','CORRECTION','OTHER')),
        status varchar(20) not null check (status in ('DRAFT','PENDING_APPROVAL','APPROVED','REJECTED','CANCELLED')),
        description varchar(500),
        rejection_reason varchar(500),
        approved_by varchar(255),
        created_by varchar(255),
        note_number varchar(255) not null unique,
        primary key (id)
    );

    create table document_sequences (
        doc_year integer not null,
        id bigserial not null,
        last_number bigint not null,
        series varchar(255) not null,
        primary key (id),
        constraint uk_document_sequence_series_year unique (series, doc_year)
    );

    create table employees (
        base_salary numeric(38,2) not null,
        contract_end_date date,
        dependents_count integer not null,
        hire_date date,
        company_id bigint not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        updated_at timestamp(6),
        employment_status varchar(20) not null,
        employee_number varchar(30),
        inss_number varchar(40),
        phone varchar(40),
        tax_id varchar(40),
        created_by varchar(255),
        department varchar(255) not null,
        email varchar(255) not null,
        name varchar(255) not null,
        role varchar(255) not null,
        primary key (id),
        unique (company_id, employee_number),
        unique (company_id, email)
    );

    create table expense_claims (
        amount numeric(38,2) not null,
        created_at timestamp(6) not null,
        employee_id bigint not null,
        id bigserial not null,
        updated_at timestamp(6),
        description varchar(500) not null,
        category varchar(255) not null,
        created_by varchar(255),
        rejection_reason varchar(255),
        status varchar(255) not null check (status in ('PENDING_APPROVAL','APPROVED','REJECTED')),
        primary key (id)
    );

    create table invoice_lines (
        discount_percentage numeric(5,2),
        line_total numeric(38,2) not null,
        quantity integer not null,
        tax_rate numeric(38,2) not null,
        unit_price numeric(38,2) not null,
        id bigserial not null,
        invoice_id bigint not null,
        product_id bigint not null,
        batch_number varchar(255),
        serial_number varchar(255),
        primary key (id)
    );

    create table invoices (
        amount_paid numeric(14,2) not null,
        tax_amount numeric(38,2) not null,
        total_amount numeric(38,2) not null,
        total_before_tax numeric(38,2) not null,
        client_id bigint not null,
        company_id bigint,
        created_at timestamp(6) not null,
        id bigserial not null,
        updated_at timestamp(6),
        warehouse_id bigint,
        cancellation_reason varchar(255),
        created_by varchar(255),
        invoice_number varchar(255) unique,
        rejection_reason varchar(255),
        sales_channel varchar(255) not null check (sales_channel in ('MANUAL','POS','ORDER')),
        status varchar(255) not null check (status in ('DRAFT','PENDING_APPROVAL','PENDING_DISCOUNT_APPROVAL','APPROVED','PARTIALLY_PAID','REJECTED','PAID','CANCELLED')),
        primary key (id)
    );

    create table payment_entries (
        amount numeric(14,2) not null,
        change_given numeric(14,2),
        tendered_amount numeric(14,2),
        created_at timestamp(6) not null,
        id bigserial not null,
        invoice_id bigint not null,
        paid_at timestamp(6) not null,
        updated_at timestamp(6),
        method varchar(20) not null check (method in ('CASH','CARD','BANK_TRANSFER','CREDIT')),
        reference varchar(100),
        created_by varchar(255),
        primary key (id)
    );

    create table payroll_irps_brackets (
        dependent_deduction numeric(14,2) not null,
        fixed_deduction numeric(14,2) not null,
        lower_bound numeric(14,2) not null,
        rate numeric(7,4) not null,
        upper_bound numeric(14,2),
        config_id bigint not null,
        id bigserial not null,
        primary key (id)
    );

    create table payroll_tax_configs (
        active boolean not null,
        effective_from date not null,
        effective_to date,
        employee_inss_rate numeric(7,4) not null,
        employer_inss_rate numeric(7,4) not null,
        company_id bigint not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        updated_at timestamp(6),
        legal_basis varchar(500),
        created_by varchar(255),
        name varchar(255) not null,
        primary key (id)
    );

    create table payslips (
        allowances numeric(14,2) not null,
        base_salary numeric(14,2) not null,
        employee_inss_rate numeric(7,4) not null,
        employer_inss numeric(14,2) not null,
        employer_inss_rate numeric(7,4) not null,
        inss_deduction numeric(14,2) not null,
        irps_deduction numeric(14,2) not null,
        irps_rate numeric(7,4) not null,
        net_pay numeric(14,2) not null,
        other_deductions numeric(14,2) not null,
        overtime numeric(14,2) not null,
        payment_date date,
        ref_month integer not null,
        ref_year integer not null,
        taxable_income numeric(14,2) not null,
        created_at timestamp(6) not null,
        employee_id bigint not null,
        id bigserial not null,
        updated_at timestamp(6),
        status varchar(20) not null,
        notes varchar(500),
        tax_legal_basis varchar(500),
        created_by varchar(255),
        payslip_number varchar(255) not null unique,
        tax_config_name varchar(255),
        primary key (id),
        unique (employee_id, ref_year, ref_month)
    );

    create table product_batches (
        entry_date date not null,
        expiration_date date not null,
        quantity numeric(12,3) not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        product_id bigint not null,
        updated_at timestamp(6),
        version bigint,
        warehouse_id bigint not null,
        batch_number varchar(255) not null,
        created_by varchar(255),
        primary key (id),
        unique (product_id, warehouse_id, batch_number)
    );

    create table product_categories (
        active boolean not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        updated_at timestamp(6),
        color_hex varchar(9),
        code varchar(30) not null unique,
        created_by varchar(255),
        name varchar(255) not null,
        primary key (id)
    );

    create table products (
        min_stock numeric(38,2),
        purchase_price numeric(38,2),
        unit_price numeric(38,2) not null,
        units_per_box integer not null,
        category_id bigint,
        created_at timestamp(6) not null,
        id bigserial not null,
        updated_at timestamp(6),
        description varchar(500),
        barcode varchar(255) unique,
        created_by varchar(255),
        name varchar(255) not null,
        reference varchar(255) unique,
        sku varchar(255) not null unique,
        primary key (id)
    );

    create table purchase_lines (
        expiration_date date,
        line_total numeric(12,2) not null,
        quantity numeric(12,3) not null,
        tax_rate numeric(4,2) not null,
        unit_price numeric(12,2) not null,
        id bigserial not null,
        product_id bigint not null,
        purchase_id bigint not null,
        batch_number varchar(255),
        serial_number varchar(255),
        primary key (id)
    );

    create table purchases (
        tax_amount numeric(12,2) not null,
        total_amount numeric(12,2) not null,
        company_id bigint not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        purchase_date timestamp(6) not null,
        supplier_id bigint not null,
        updated_at timestamp(6),
        warehouse_id bigint not null,
        created_by varchar(255),
        purchase_number varchar(255) not null unique,
        status varchar(255) not null,
        primary key (id)
    );

    create table receipts (
        amount_paid numeric(12,2) not null,
        company_id bigint not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        invoice_id bigint not null,
        receipt_date timestamp(6) not null,
        treasury_account_id bigint not null,
        updated_at timestamp(6),
        cancellation_reason varchar(255),
        created_by varchar(255),
        payment_method varchar(255) not null,
        receipt_number varchar(255) not null unique,
        status varchar(255) not null,
        primary key (id)
    );

    create table stock_movements (
        quantity numeric(12,3) not null,
        batch_id bigint,
        created_at timestamp(6) not null,
        id bigserial not null,
        movement_date timestamp(6) not null,
        product_id bigint not null,
        updated_at timestamp(6),
        warehouse_id bigint not null,
        batch_number varchar(255),
        created_by varchar(255),
        description varchar(255),
        movement_type varchar(255) not null check (movement_type in ('PURCHASE','ENTRY','SALE','TRANSFER','ADJUSTMENT','RETURN','REVERSAL')),
        serial_number varchar(255),
        primary key (id)
    );

    create table stock_transfer_lines (
        quantity numeric(12,3) not null,
        id bigserial not null,
        product_id bigint not null,
        transfer_id bigint not null,
        batch_number varchar(255),
        primary key (id)
    );

    create table stock_transfers (
        company_id bigint not null,
        created_at timestamp(6) not null,
        destination_warehouse_id bigint not null,
        id bigserial not null,
        origin_warehouse_id bigint not null,
        transfer_date timestamp(6) not null,
        updated_at timestamp(6),
        status varchar(20) not null,
        notes varchar(500),
        created_by varchar(255),
        responsible varchar(255),
        transfer_number varchar(255) not null unique,
        vehicle varchar(255),
        primary key (id)
    );

    create table stocks (
        quantity numeric(12,3) not null,
        id bigserial not null,
        product_id bigint not null,
        version bigint,
        warehouse_id bigint not null,
        primary key (id),
        unique (product_id, warehouse_id)
    );

    create table suppliers (
        company_id bigint not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        updated_at timestamp(6),
        address varchar(255),
        created_by varchar(255),
        email varchar(255),
        name varchar(255) not null,
        tax_id varchar(255) not null,
        primary key (id)
    );

    create table tax_rates (
        active boolean not null,
        rate numeric(7,4) not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        updated_at timestamp(6),
        code varchar(30) not null,
        type varchar(30) not null check (type in ('IVA_STANDARD','IVA_REDUCED','IVA_ZERO','IVA_EXEMPT','WITHHOLDING','CORPORATE_INCOME','EXCISE')),
        legal_basis varchar(300),
        created_by varchar(255),
        name varchar(255) not null,
        primary key (id),
        unique (code)
    );

    create table till_movements (
        amount numeric(12,2) not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        movement_date timestamp(6) not null,
        till_session_id bigint not null,
        updated_at timestamp(6),
        created_by varchar(255),
        description varchar(255),
        movement_type varchar(255) not null check (movement_type in ('SALE','SUPRIMENTO','SANGRIA')),
        primary key (id)
    );

    create table till_sessions (
        closing_balance_expected numeric(12,2),
        closing_balance_real numeric(12,2),
        difference numeric(12,2),
        opening_balance numeric(12,2) not null,
        close_date timestamp(6),
        company_id bigint not null,
        id bigserial not null,
        open_date timestamp(6) not null,
        operator varchar(255) not null,
        status varchar(255) not null,
        primary key (id)
    );

    create table treasury_accounts (
        balance numeric(38,2) not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        updated_at timestamp(6),
        version bigint,
        account_number varchar(255),
        created_by varchar(255),
        name varchar(255) not null,
        primary key (id)
    );

    create table treasury_transactions (
        amount numeric(38,2) not null,
        account_id bigint not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        transaction_date timestamp(6) not null,
        updated_at timestamp(6),
        description varchar(500) not null,
        created_by varchar(255),
        transaction_type varchar(255) not null check (transaction_type in ('DEBIT','CREDIT')),
        primary key (id)
    );

    create table vacations (
        end_date date not null,
        start_date date not null,
        total_days integer not null,
        year_reference integer not null,
        created_at timestamp(6) not null,
        decision_at timestamp(6),
        employee_id bigint not null,
        id bigserial not null,
        updated_at timestamp(6),
        status varchar(20) not null,
        notes varchar(500),
        rejection_reason varchar(500),
        created_by varchar(255),
        decision_by varchar(255),
        primary key (id)
    );

    create table warehouses (
        capacity numeric(12,3),
        company_id bigint not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        updated_at timestamp(6),
        created_by varchar(255),
        location varchar(255),
        name varchar(255) not null,
        warehouse_number varchar(255),
        primary key (id)
    );

    create table withholding_records (
        base_amount numeric(14,2) not null,
        delivered_at date,
        net_paid numeric(14,2) not null,
        record_date date not null,
        tax_rate numeric(5,4) not null,
        withheld_amount numeric(14,2) not null,
        company_id bigint not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        updated_at timestamp(6),
        status varchar(20) not null,
        beneficiary_tax_id varchar(30),
        tax_category varchar(50),
        service_description varchar(300) not null,
        beneficiary_name varchar(255) not null,
        created_by varchar(255),
        primary key (id)
    );

    create index idx_batch_fefo 
       on product_batches (product_id, warehouse_id, expiration_date);

    alter table if exists absences 
       add constraint FKlklcclr8g2uoy15595kww1uo8 
       foreign key (employee_id) 
       references employees;

    alter table if exists approval_history 
       add constraint FKdw31djs8pq42nrqikf2gvduhi 
       foreign key (approval_request_id) 
       references approval_requests;

    alter table if exists credit_note_lines 
       add constraint FKhc7gj5bbecw36l3577howbbfv 
       foreign key (credit_note_id) 
       references credit_notes;

    alter table if exists credit_note_lines 
       add constraint FKknrqqnni6w1bfo5op6rmu5qug 
       foreign key (invoice_line_id) 
       references invoice_lines;

    alter table if exists credit_note_lines 
       add constraint FKs7vb01v0gwnmm45j9beqil963 
       foreign key (product_id) 
       references products;

    alter table if exists credit_notes 
       add constraint FK7ueh4soyrek6tyugl3mm4qxci 
       foreign key (client_id) 
       references clients;

    alter table if exists credit_notes 
       add constraint FK4kn505m7rd4qjomvm1ocgk37a 
       foreign key (company_id) 
       references companies;

    alter table if exists credit_notes 
       add constraint FK17v6ewtnp6crd16dih7jdopev 
       foreign key (invoice_id) 
       references invoices;

    alter table if exists credit_notes 
       add constraint FK62nen38qpo91j226r4y9lph9a 
       foreign key (warehouse_id) 
       references warehouses;

    alter table if exists crm_tickets 
       add constraint FKan5um7k6hqsk26a88av3b8cf7 
       foreign key (client_id) 
       references clients;

    alter table if exists crm_work_sheets 
       add constraint FKb3bh169ttgjifo5iwiphfqol0 
       foreign key (ticket_id) 
       references crm_tickets;

    alter table if exists customer_order_lines 
       add constraint FKlvfckum3fgm9iarc3wfg69fut 
       foreign key (order_id) 
       references customer_orders;

    alter table if exists customer_order_lines 
       add constraint FKdwpiw4cnkooa070yv6ed64459 
       foreign key (product_id) 
       references products;

    alter table if exists customer_orders 
       add constraint FKonnw8lh7kdy4btkw87qcxy7kr 
       foreign key (client_id) 
       references clients;

    alter table if exists customer_orders 
       add constraint FK40u1ksmn8wpykmui6x0r5tuwy 
       foreign key (company_id) 
       references companies;

    alter table if exists customer_orders 
       add constraint FKnc45e4aodvaown0ni7t9fx6g6 
       foreign key (warehouse_id) 
       references warehouses;

    alter table if exists debit_note_lines 
       add constraint FK6svic1aj4iweqtvqg3xfjkxtr 
       foreign key (debit_note_id) 
       references debit_notes;

    alter table if exists debit_notes 
       add constraint FK3d95oxw3hd4r5a8n6qh3hke8x 
       foreign key (client_id) 
       references clients;

    alter table if exists debit_notes 
       add constraint FK3ru3e7v1dwtqsop8n09orwuwb 
       foreign key (company_id) 
       references companies;

    alter table if exists debit_notes 
       add constraint FKj5mu1b9u6ie0o4ebcvy1ycb7n 
       foreign key (invoice_id) 
       references invoices;

    alter table if exists employees 
       add constraint FK1ekpcbo0lmdx6ou8e3fh9j4lq 
       foreign key (company_id) 
       references companies;

    alter table if exists expense_claims 
       add constraint FKgxbaysh3098yxh3i7ti3kerk7 
       foreign key (employee_id) 
       references employees;

    alter table if exists invoice_lines 
       add constraint FKsgudq2lwpa9wc92a23nggah1w 
       foreign key (invoice_id) 
       references invoices;

    alter table if exists invoice_lines 
       add constraint FKm2jo8loc0ps5q6qtlx4nx6o3e 
       foreign key (product_id) 
       references products;

    alter table if exists invoices 
       add constraint FK9ioqm804urbgy986pdtwqtl0x 
       foreign key (client_id) 
       references clients;

    alter table if exists invoices 
       add constraint FK9uwtrg1887fbqa4gb98n6hik6 
       foreign key (company_id) 
       references companies;

    alter table if exists invoices 
       add constraint FK2gw7glghekg32lvpmeimn0lys 
       foreign key (warehouse_id) 
       references warehouses;

    alter table if exists payment_entries 
       add constraint FK5gajttintjocemknqkrmx83s5 
       foreign key (invoice_id) 
       references invoices;

    alter table if exists payroll_irps_brackets 
       add constraint FK8ka71x2h58bm6xwh4he8ynhik 
       foreign key (config_id) 
       references payroll_tax_configs;

    alter table if exists payroll_tax_configs 
       add constraint FK3uedns760jr4sbg9wr8wv5v6r 
       foreign key (company_id) 
       references companies;

    alter table if exists payslips 
       add constraint FKi2u90djkfkqooebb9b26gxqmi 
       foreign key (employee_id) 
       references employees;

    alter table if exists product_batches 
       add constraint FKo2hwf6cltkf4qkdim5w29rbgq 
       foreign key (product_id) 
       references products;

    alter table if exists product_batches 
       add constraint FK5xdgr3bec56qvt7d60dx3so2t 
       foreign key (warehouse_id) 
       references warehouses;

    alter table if exists products 
       add constraint FK6t5dtw6tyo83ywljwohuc6g7k 
       foreign key (category_id) 
       references product_categories;

    alter table if exists purchase_lines 
       add constraint FKgm8ptmjl3xm9pkwvv5wgymryr 
       foreign key (product_id) 
       references products;

    alter table if exists purchase_lines 
       add constraint FK6g7pnc6oq338s465fh8ym425w 
       foreign key (purchase_id) 
       references purchases;

    alter table if exists purchases 
       add constraint FKlohs2ream9b5klunl8j86g55d 
       foreign key (company_id) 
       references companies;

    alter table if exists purchases 
       add constraint FK9ho3w23v5du4x0hrp6rqs1wmh 
       foreign key (supplier_id) 
       references suppliers;

    alter table if exists purchases 
       add constraint FKo5ayqstf3r4ec39aykkavuscp 
       foreign key (warehouse_id) 
       references warehouses;

    alter table if exists receipts 
       add constraint FKjutvdfijb78o4esanjcpht080 
       foreign key (company_id) 
       references companies;

    alter table if exists receipts 
       add constraint FK3hmid8b40s5yd0jo2s36684ql 
       foreign key (invoice_id) 
       references invoices;

    alter table if exists receipts 
       add constraint FK932j86mrmvlupnl58o2tyad9l 
       foreign key (treasury_account_id) 
       references treasury_accounts;

    alter table if exists stock_movements 
       add constraint FKqpfwbaxwvri2cxtahci1qjq6e 
       foreign key (batch_id) 
       references product_batches;

    alter table if exists stock_movements 
       add constraint FKjcaag8ogfjxpwmqypi1wfdaog 
       foreign key (product_id) 
       references products;

    alter table if exists stock_movements 
       add constraint FKiparp4rp4rsfsxb9y02oyxauh 
       foreign key (warehouse_id) 
       references warehouses;

    alter table if exists stock_transfer_lines 
       add constraint FKomr5f22f88s8bdu4a3hfm4lxf 
       foreign key (product_id) 
       references products;

    alter table if exists stock_transfer_lines 
       add constraint FKne9bq28olbi5egc1nk841837r 
       foreign key (transfer_id) 
       references stock_transfers;

    alter table if exists stock_transfers 
       add constraint FK7igwfpfm5e0fxop2thk7jtbou 
       foreign key (company_id) 
       references companies;

    alter table if exists stock_transfers 
       add constraint FK24aoj1hs71g3vit38m2ut0n3c 
       foreign key (destination_warehouse_id) 
       references warehouses;

    alter table if exists stock_transfers 
       add constraint FKou0ylj58jsx7fla0oo51w44ag 
       foreign key (origin_warehouse_id) 
       references warehouses;

    alter table if exists stocks 
       add constraint FKff7be959jyco0iukc1dcjj9qm 
       foreign key (product_id) 
       references products;

    alter table if exists stocks 
       add constraint FKjftt43i266337pt7y8b291hpx 
       foreign key (warehouse_id) 
       references warehouses;

    alter table if exists suppliers 
       add constraint FKsh0gkhht4jenmvxp5io5gr1we 
       foreign key (company_id) 
       references companies;

    alter table if exists till_movements 
       add constraint FKtmwjt11yppks905gi5p7qqexf 
       foreign key (till_session_id) 
       references till_sessions;

    alter table if exists till_sessions 
       add constraint FKj2hmdak75tf9wa12q1f05locv 
       foreign key (company_id) 
       references companies;

    alter table if exists treasury_transactions 
       add constraint FKqbwmxt10dla5ak6vsc7e0qpus 
       foreign key (account_id) 
       references treasury_accounts;

    alter table if exists vacations 
       add constraint FK2fw7ungsht4mw0o3xvh6u5kth 
       foreign key (employee_id) 
       references employees;

    alter table if exists warehouses 
       add constraint FK6ycscv7ubtal0aevf2aapf4m9 
       foreign key (company_id) 
       references companies;

    alter table if exists withholding_records 
       add constraint FK4i85oie85v0qs8caogky8b1v9 
       foreign key (company_id) 
       references companies;
