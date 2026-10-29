import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** The small domain decision: confirmed appointments get a patient-safe reminder. */
public final class AppointmentWorkflow {
    public enum Status { REQUESTED, CONFIRMED, CANCELLED }

    public record Appointment(String id, String patientId, String clinician, Instant startsAt, Status status) {}
    public record Notification(String appointmentId, String recipient, String message) {}

    public Notification reminderFor(Appointment appointment) {
        if (appointment.status() != Status.CONFIRMED) {
            return null;
        }
        String message = "Your appointment with " + appointment.clinician() + " is confirmed for " + appointment.startsAt();
        return new Notification(appointment.id(), appointment.patientId(), message);
    }

    public Map<String, Object> logFields(Appointment appointment, Notification notification) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("message", "Appointment confirmation workflow event");
        fields.put("level", "info");
        fields.put("appointment_id", appointment.id());
        fields.put("patient_id", appointment.patientId());
        fields.put("workflow", "appointment_confirmation");
        fields.put("notification", notification == null ? "not_sent" : "sent");
        fields.put("clinician", appointment.clinician());
        return fields;
    }
}
