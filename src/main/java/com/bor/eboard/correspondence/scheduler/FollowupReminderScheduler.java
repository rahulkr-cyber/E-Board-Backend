package com.bor.eboard.correspondence.scheduler;

import com.bor.eboard.correspondence.entity.DispatchRegisterEntry;
import com.bor.eboard.correspondence.entity.Followup;
import com.bor.eboard.correspondence.repository.DispatchRepository;
import com.bor.eboard.correspondence.repository.FollowupRepository;
import com.bor.eboard.notification.facade.NotificationFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * BCR-03 Part 10: generates dispatch follow-up reminders automatically.
 *
 * <p>Runs once a day. For every open follow-up whose reminder date has arrived
 * and where no reply has been received, the dispatch creator is notified. The
 * follow-up is then marked REMINDED so the same reminder does not fire twice —
 * the same de-duplication idea used by the escalation scheduler.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FollowupReminderScheduler {

    private static final String STATUS_REMINDED = "REMINDED";

    private final FollowupRepository followupRepository;
    private final DispatchRepository dispatchRepository;
    private final NotificationFacade notificationFacade;

    @Scheduled(cron = "${followup.reminder.cron:0 30 1 * * *}")
    @Transactional
    public void runDailyFollowupReminders() {
        LocalDate today = LocalDate.now();
        List<Followup> due = followupRepository.findDueReminders(today);
        if (due.isEmpty()) {
            return;
        }
        log.info("Follow-up reminder sweep: {} follow-up(s) due", due.size());

        for (Followup followup : due) {
            if (STATUS_REMINDED.equals(followup.getStatus())) {
                continue;   // already reminded
            }

            DispatchRegisterEntry dispatch = followup.getDispatchId() == null ? null
                    : dispatchRepository.findByIdAndDeletedFalse(followup.getDispatchId())
                            .orElse(null);
            if (dispatch == null) {
                continue;
            }

            // Notify whoever raised the dispatch; they own chasing the reply.
            java.util.UUID recipient = dispatch.getCreatedBy();
            if (recipient == null) {
                continue;
            }

            long overdueDays = followup.getDueDate() == null ? 0L
                    : java.time.temporal.ChronoUnit.DAYS
                            .between(followup.getDueDate(), today);
            String priority = overdueDays > 0 ? "HIGH" : "NORMAL";

            notificationFacade.notify(recipient,
                    "Follow-up due: " + followup.getFollowupNumber(),
                    "No reply yet for dispatch " + dispatch.getDispatchNumber()
                            + " to " + dispatch.getRecipientName()
                            + (overdueDays > 0
                                    ? " — overdue by " + overdueDays + " day(s)."
                                    : " — due today."),
                    "REMINDER", "DISPATCH", dispatch.getId(), priority);

            followup.setStatus(STATUS_REMINDED);
            followupRepository.save(followup);
        }
    }
}
