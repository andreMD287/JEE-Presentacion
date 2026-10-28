package co.edu.javeriana.recursos.contracts;

import co.edu.javeriana.recursos.contracts.dto.CreateResourceRequestCommand;
import co.edu.javeriana.recursos.contracts.dto.Priority;
import co.edu.javeriana.recursos.contracts.dto.RequestStatus;
import co.edu.javeriana.recursos.contracts.dto.ResourceRequestData;
import co.edu.javeriana.recursos.contracts.dto.ResourceRequestView;
import co.edu.javeriana.recursos.contracts.exception.AlreadyAttendedException;
import co.edu.javeriana.recursos.contracts.exception.BusinessException;
import co.edu.javeriana.recursos.contracts.exception.BusinessValidationException;
import co.edu.javeriana.recursos.contracts.exception.RequestNotFoundException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Todo lo que cruza una frontera remota debe sobrevivir a la serialización.
 * (La prueba real con el marshalling de WildFly se hace en el spike S-2.)
 */
class ContractsSerializationTest {

    @Test
    void dtosSurviveSerialization() throws Exception {
        var command = new CreateResourceRequestCommand("Inundacion en Mocoa", "Agua", 100, "ALTA");
        var view = new ResourceRequestView(1L, "Inundacion en Mocoa", "Agua", 100,
                Priority.ALTA, RequestStatus.PENDIENTE);
        var data = new ResourceRequestData(1L, "Inundacion en Mocoa", "Agua", 100,
                Priority.ALTA, RequestStatus.ATENDIDA);

        assertEquals(command, roundTrip(command));
        assertEquals(view, roundTrip(view));
        assertEquals(data, roundTrip(data));
    }

    @Test
    void businessExceptionsKeepCodeAndMessage() throws Exception {
        assertSameError(new BusinessValidationException("La cantidad debe ser mayor que cero"),
                "VALIDATION_ERROR");
        assertSameError(new RequestNotFoundException(7L), "REQUEST_NOT_FOUND");
        assertSameError(new AlreadyAttendedException(7L), "ALREADY_ATTENDED");
    }

    @Test
    void jndiNamesFollowWildFlyFormat() {
        assertEquals("ejb:/logic/ResourceRequestServiceBean!"
                + "co.edu.javeriana.recursos.contracts.ResourceRequestService", JndiNames.LOGIC_SERVICE);
        assertEquals("ejb:/data/ResourceRequestDataServiceBean!"
                + "co.edu.javeriana.recursos.contracts.ResourceRequestDataService", JndiNames.DATA_SERVICE);
    }

    private static void assertSameError(BusinessException original, String expectedCode) throws Exception {
        BusinessException copy = roundTrip(original);
        assertInstanceOf(original.getClass(), copy);
        assertEquals(expectedCode, copy.getCode());
        assertEquals(original.getMessage(), copy.getMessage());
    }

    @SuppressWarnings("unchecked")
    private static <T extends Serializable> T roundTrip(T value) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        try (var in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) in.readObject();
        }
    }
}
