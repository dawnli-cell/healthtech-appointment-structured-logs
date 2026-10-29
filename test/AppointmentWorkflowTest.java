import java.time.Instant;

public final class AppointmentWorkflowTest {
    public static void main(String[] args) {
        AppointmentWorkflow workflow = new AppointmentWorkflow();
        AppointmentWorkflow.Appointment confirmed = new AppointmentWorkflow.Appointment(
                "apt-test", "patient-test", "Dr. Lin", Instant.parse("2026-11-03T09:00:00Z"), AppointmentWorkflow.Status.CONFIRMED);
        AppointmentWorkflow.Appointment requested = new AppointmentWorkflow.Appointment(
                "apt-pending", "patient-test", "Dr. Lin", confirmed.startsAt(), AppointmentWorkflow.Status.REQUESTED);
        require(workflow.reminderFor(confirmed) != null, "confirmed appointment sends a reminder");
        require(workflow.reminderFor(requested) == null, "requested appointment sends no reminder");
        System.out.println("AppointmentWorkflowTest passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
