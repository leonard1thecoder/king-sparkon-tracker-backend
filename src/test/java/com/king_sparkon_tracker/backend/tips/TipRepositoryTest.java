package com.king_sparkon_tracker.backend.tips;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.Tip;
import com.king_sparkon_tracker.backend.model.TipStatus;
import com.king_sparkon_tracker.backend.model.TipWithdrawal;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.TipRepository;

@DataJpaTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class TipRepositoryTest {

	@Autowired
	private TipRepository tipRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void findByWorkerIdOrderByCreatedDescReturnsNewestTipFirst() {
		Privilege workerRole = privilege(PrivilegeRole.Worker);
		TrackerUser worker = user("worker", workerRole);
		TrackerUser otherWorkerUser = user("other-worker", workerRole);
		Tip older = tip(worker, "25.00", "2026-06-21T10:00:00+02:00", TipStatus.UNPAID);
		Tip newer = tip(worker, "50.00", "2026-06-23T10:00:00+02:00", TipStatus.UNPAID);
		Tip otherWorker = tip(otherWorkerUser, "75.00", "2026-06-24T10:00:00+02:00", TipStatus.UNPAID);
		tipRepository.saveAll(List.of(older, newer, otherWorker));

		List<Tip> tips = tipRepository.findByWorker_IdOrderByCreatedDesc(worker.getId());

		assertThat(tips).extracting(Tip::getTipAmount)
				.containsExactly(new BigDecimal("50.00"), new BigDecimal("25.00"));
	}

	@Test
	void findByStatusReturnsMatchingTips() {
		Privilege workerRole = privilege(PrivilegeRole.Worker);
		TrackerUser unpaidWorker = user("unpaid-worker", workerRole);
		TrackerUser paidWorker = user("paid-worker", workerRole);
		Tip unpaid = tip(unpaidWorker, "25.00", "2026-06-21T10:00:00+02:00", TipStatus.UNPAID);
		Tip paid = tip(paidWorker, "50.00", "2026-06-22T10:00:00+02:00", TipStatus.PAID);
		tipRepository.saveAll(List.of(unpaid, paid));

		assertThat(tipRepository.findByStatus(TipStatus.PAID))
				.extracting(Tip::getWorkerId)
				.containsExactly(paidWorker.getId());
	}

	@Test
	void findWithdrawableTipsReturnsOnlyPaidOldUnwithdrawnTipsForWorker() {
		Privilege workerRole = privilege(PrivilegeRole.Worker);
		Privilege ownerRole = privilege(PrivilegeRole.Owner);
		TrackerUser worker = user("withdraw-worker", workerRole);
		TrackerUser otherWorkerUser = user("withdraw-other-worker", workerRole);
		TrackerUser owner = user("owner", ownerRole);
		TipWithdrawal withdrawal = new TipWithdrawal(
				worker,
				owner,
				new BigDecimal("700.00"),
				"ZAR",
				1,
				"worker@example.com");
		entityManager.persist(withdrawal);

		Tip eligible = tip(worker, "1200.00", "2026-06-10T10:00:00+02:00", TipStatus.PAID);
		Tip tooRecent = tip(worker, "900.00", "2026-06-22T10:00:00+02:00", TipStatus.PAID);
		Tip unpaid = tip(worker, "800.00", "2026-06-09T10:00:00+02:00", TipStatus.UNPAID);
		Tip alreadyWithdrawn = tip(worker, "700.00", "2026-06-08T10:00:00+02:00", TipStatus.PAID);
		alreadyWithdrawn.assignWithdrawal(withdrawal);
		Tip otherWorker = tip(otherWorkerUser, "1300.00", "2026-06-07T10:00:00+02:00", TipStatus.PAID);
		tipRepository.saveAll(List.of(eligible, tooRecent, unpaid, alreadyWithdrawn, otherWorker));

		List<Tip> tips = tipRepository.findByWorker_IdAndStatusAndWithdrawalIsNullAndCreatedLessThanEqualOrderByCreatedDesc(
				worker.getId(),
				TipStatus.PAID,
				OffsetDateTime.parse("2026-06-16T10:00:00+02:00"));

		assertThat(tips).extracting(Tip::getTipAmount)
				.containsExactly(new BigDecimal("1200.00"));
	}

	private Tip tip(TrackerUser worker, String amount, String created, TipStatus status) {
		Tip tip = new Tip(worker, new BigDecimal(amount));
		ReflectionTestUtils.setField(tip, "created", OffsetDateTime.parse(created));
		ReflectionTestUtils.setField(tip, "updated", OffsetDateTime.parse(created));
		ReflectionTestUtils.setField(tip, "status", status);
		return tip;
	}

	private Privilege privilege(PrivilegeRole role) {
		Privilege privilege = new Privilege(role);
		entityManager.persist(privilege);
		return privilege;
	}

	private TrackerUser user(String username, Privilege privilege) {
		TrackerUser user = new TrackerUser(username, username + "@example.com", "encoded", privilege);
		entityManager.persist(user);
		return user;
	}
}
