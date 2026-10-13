package dan.edu.payment_service.service;

import java.util.Locale;
import java.util.UUID;

import dan.edu.payment_service.common.ConflictException;
import dan.edu.payment_service.common.InvalidPaymentStateException;
import dan.edu.payment_service.common.ResourceNotFoundException;
import dan.edu.payment_service.dto.CreatePaymentRequest;
import dan.edu.payment_service.dto.PaymentResponse;
import dan.edu.payment_service.dto.SimulatePaymentRequest;
import dan.edu.payment_service.event.OrderCreatedEvent;
import dan.edu.payment_service.model.Payment;
import dan.edu.payment_service.utils.enums.PaymentMethod;
import dan.edu.payment_service.repository.PaymentRepository;
import dan.edu.payment_service.utils.mapper.PaymentMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

	private final PaymentRepository paymentRepository;
	private final PaymentMapper paymentMapper;
	private final OrderPaymentEventPublisher orderPaymentEventPublisher;
	private final boolean mockEnabled;

	public PaymentService(
			PaymentRepository paymentRepository,
			PaymentMapper paymentMapper,
			OrderPaymentEventPublisher orderPaymentEventPublisher,
			@Value("${payment.mock.enabled:false}") boolean mockEnabled) {
		this.paymentRepository = paymentRepository;
		this.paymentMapper = paymentMapper;
		this.orderPaymentEventPublisher = orderPaymentEventPublisher;
		this.mockEnabled = mockEnabled;
	}

	@Transactional
	public PaymentResponse createFromOrderCreated(OrderCreatedEvent event) {
		if (event == null || event.orderId() == null || event.userId() == null
				|| event.totalAmount() == null || event.currency() == null) {
			return null;
		}
		return paymentRepository.findByOrderIdAndUserId(event.orderId(), event.userId())
				.map(paymentMapper::toResponse)
				.orElseGet(() -> create(event.userId(), new CreatePaymentRequest(
						event.orderId(),
						event.totalAmount(),
						event.currency(),
						PaymentMethod.CARD,
						"order-created:" + event.orderId())));
	}

	@Transactional
	public PaymentResponse create(UUID userId, CreatePaymentRequest request) {
		String currency = request.currency().toUpperCase(Locale.ROOT);
		return paymentRepository.findByUserIdAndIdempotencyKey(userId, request.idempotencyKey())
				.map(existing -> {
					if (!existing.matches(userId, request.orderId(), request.amount(), currency, request.method())) {
						throw new ConflictException("The idempotency key was already used for a different payment");
					}
					return paymentMapper.toResponse(existing);
				})
				.orElseGet(() -> paymentMapper.toResponse(paymentRepository.save(new Payment(
						userId,
						request.orderId(),
						request.amount(),
						currency,
						request.method(),
						request.idempotencyKey()))));
	}

	@Transactional(readOnly = true)
	public PaymentResponse get(UUID userId, UUID paymentId) {
		return paymentMapper.toResponse(findOwnedPayment(paymentId, userId));
	}

	@Transactional(readOnly = true)
	public PaymentResponse getByOrder(UUID userId, UUID orderId) {
		Payment payment = paymentRepository.findByOrderIdAndUserId(orderId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
		return paymentMapper.toResponse(payment);
	}

	@Transactional(readOnly = true)
	public Page<PaymentResponse> list(UUID userId, Pageable pageable) {
		return paymentRepository.findAllByUserId(userId, pageable).map(paymentMapper::toResponse);
	}

	@Transactional
	public PaymentResponse simulate(UUID userId, UUID paymentId, SimulatePaymentRequest request) {
		if (!mockEnabled) {
			throw new InvalidPaymentStateException("Payment simulation is disabled");
		}
		Payment payment = findOwnedPaymentForUpdate(paymentId, userId);
		payment.simulate(request.outcome());
		PaymentResponse response = paymentMapper.toResponse(payment);
		orderPaymentEventPublisher.publish(response, userId);
		return response;
	}

	@Transactional
	public PaymentResponse cancel(UUID userId, UUID paymentId) {
		Payment payment = findOwnedPaymentForUpdate(paymentId, userId);
		payment.cancel();
		PaymentResponse response = paymentMapper.toResponse(payment);
		orderPaymentEventPublisher.publish(response, userId);
		return response;
	}

	@Transactional
	public void cancelForOrder(UUID userId, UUID orderId) {
		paymentRepository.findByOrderIdAndUserIdForUpdate(orderId, userId)
				.ifPresent(Payment::cancel);
	}

	@Transactional
	public PaymentResponse refund(UUID paymentId) {
		Payment payment = paymentRepository.findByIdForUpdate(paymentId)
				.orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
		payment.refund();
		return paymentMapper.toResponse(payment);
	}

	private Payment findOwnedPayment(UUID paymentId, UUID userId) {
		return paymentRepository.findByIdAndUserId(paymentId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
	}

	private Payment findOwnedPaymentForUpdate(UUID paymentId, UUID userId) {
		return paymentRepository.findByIdAndUserIdForUpdate(paymentId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
	}
}
