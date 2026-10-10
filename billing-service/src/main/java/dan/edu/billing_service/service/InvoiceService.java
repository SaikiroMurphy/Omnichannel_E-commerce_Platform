package dan.edu.billing_service.service;

import java.util.UUID;

import dan.edu.billing_service.common.ConflictException;
import dan.edu.billing_service.common.ResourceNotFoundException;
import dan.edu.billing_service.dto.CreateInvoiceRequest;
import dan.edu.billing_service.dto.InvoiceResponse;
import dan.edu.billing_service.model.Invoice;
import dan.edu.billing_service.repository.InvoiceRepository;
import dan.edu.billing_service.utils.mapper.InvoiceMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

	private final InvoiceRepository invoiceRepository;
	private final InvoiceMapper invoiceMapper;

	public InvoiceService(InvoiceRepository invoiceRepository, InvoiceMapper invoiceMapper) {
		this.invoiceRepository = invoiceRepository;
		this.invoiceMapper = invoiceMapper;
	}

	@Transactional
	public InvoiceResponse create(CreateInvoiceRequest request) {
		if (invoiceRepository.existsByOrderId(request.orderId())) {
			throw new ConflictException("An invoice already exists for this order");
		}
		return invoiceMapper.toResponse(invoiceRepository.save(new Invoice(request)));
	}

	@Transactional(readOnly = true)
	public Page<InvoiceResponse> listForCustomer(UUID customerId, Pageable pageable) {
		return invoiceRepository.findAllByCustomerId(customerId, pageable).map(invoiceMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public Page<InvoiceResponse> listAll(Pageable pageable) {
		return invoiceRepository.findAll(pageable).map(invoiceMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public InvoiceResponse get(UUID invoiceId, UUID customerId, boolean admin) {
		Invoice invoice = admin
				? invoiceRepository.findById(invoiceId)
						.orElseThrow(() -> new ResourceNotFoundException("Invoice not found"))
				: invoiceRepository.findByIdAndCustomerId(invoiceId, customerId)
						.orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
		return invoiceMapper.toResponse(invoice);
	}

	@Transactional(readOnly = true)
	public InvoiceResponse getByOrder(UUID orderId, UUID customerId, boolean admin) {
		Invoice invoice = admin
				? invoiceRepository.findByOrderId(orderId)
						.orElseThrow(() -> new ResourceNotFoundException("Invoice not found"))
				: invoiceRepository.findByOrderIdAndCustomerId(orderId, customerId)
						.orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
		return invoiceMapper.toResponse(invoice);
	}

	@Transactional
	public InvoiceResponse issue(UUID invoiceId) {
		Invoice invoice = findForUpdate(invoiceId);
		invoice.issue();
		return invoiceMapper.toResponse(invoice);
	}

	@Transactional
	public InvoiceResponse voidInvoice(UUID invoiceId) {
		Invoice invoice = findForUpdate(invoiceId);
		invoice.voidInvoice();
		return invoiceMapper.toResponse(invoice);
	}

	private Invoice findForUpdate(UUID invoiceId) {
		return invoiceRepository.findByIdForUpdate(invoiceId)
				.orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
	}
}
