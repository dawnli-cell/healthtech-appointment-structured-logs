import java.time.Instant;
import java.util.Map;

/** Runnable path for a course lesson: decide, publish a structured log, then search it. */
public final class HealthtechJob {
    public static void main(String[] args) throws Exception {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("Set INFRAI_API_KEY before running the job");

        AppointmentWorkflow workflow = new AppointmentWorkflow();
        AppointmentWorkflow.Appointment appointment = new AppointmentWorkflow.Appointment(
                "apt-lesson-01", "patient-17", "Dr. Chen", Instant.parse("2026-11-03T09:00:00Z"),
                AppointmentWorkflow.Status.CONFIRMED);
        AppointmentWorkflow.Notification notification = workflow.reminderFor(appointment);
        Map<String, Object> fields = workflow.logFields(appointment, notification);
        InfraiClient infrai = new InfraiClient(key);

        System.out.println("notification=" + (notification == null ? "not sent" : notification.message()));
        System.out.println("log=" + infrai.ingestLog(fields));
        System.out.println("audit credential context=" + infrai.listKeys());
        System.out.println("search=" + infrai.searchLogs("appointment_id=apt-lesson-01"));
    }
}
