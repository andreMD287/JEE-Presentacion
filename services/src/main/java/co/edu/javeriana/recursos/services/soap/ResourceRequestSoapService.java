package co.edu.javeriana.recursos.services.soap;

import co.edu.javeriana.recursos.contracts.ResourceRequestService;
import co.edu.javeriana.recursos.contracts.dto.CreateResourceRequestCommand;
import co.edu.javeriana.recursos.contracts.dto.ResourceRequestView;
import co.edu.javeriana.recursos.contracts.exception.BusinessException;
import co.edu.javeriana.recursos.services.client.LogicClient;
import co.edu.javeriana.recursos.services.client.DependencyAvailability;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

import java.util.List;
import java.util.function.Supplier;

/** Exposición SOAP de la misma Lógica consumida por REST y JSF. */
@WebService(serviceName = "ResourceRequestSoapService", portName = "ResourceRequestSoapPort",
        name = "ResourceRequestSoapService", targetNamespace = ResourceRequestSoapService.NAMESPACE)
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL,
        parameterStyle = SOAPBinding.ParameterStyle.WRAPPED)
public class ResourceRequestSoapService {
    public static final String NAMESPACE = "http://recursos.javeriana.edu.co/soap";
    private final Supplier<ResourceRequestService> services;

    public ResourceRequestSoapService() {
        this(LogicClient::get);
    }

    ResourceRequestSoapService(Supplier<ResourceRequestService> services) {
        this.services = services;
    }

    @WebMethod(operationName = "registerRequest")
    @WebResult(name = "request")
    public ResourceRequestView registerRequest(@WebParam(name = "request") CreateResourceRequestCommand request)
            throws BusinessFault {
        try {
            return services.get().register(request);
        } catch (BusinessException e) {
            throw businessFault(e);
        } catch (RuntimeException e) {
            throw internalFault(e);
        }
    }

    @WebMethod(operationName = "listRequests")
    @WebResult(name = "request")
    public List<ResourceRequestView> listRequests() throws BusinessFault {
        try {
            return services.get().findAll();
        } catch (BusinessException e) {
            throw businessFault(e);
        } catch (RuntimeException e) {
            throw internalFault(e);
        }
    }

    @WebMethod(operationName = "getRequest")
    @WebResult(name = "request")
    public ResourceRequestView getRequest(@WebParam(name = "id") Long id) throws BusinessFault {
        try {
            return services.get().findById(id);
        } catch (BusinessException e) {
            throw businessFault(e);
        } catch (RuntimeException e) {
            throw internalFault(e);
        }
    }

    @WebMethod(operationName = "attendRequest")
    @WebResult(name = "request")
    public ResourceRequestView attendRequest(@WebParam(name = "id") Long id) throws BusinessFault {
        try {
            return services.get().attend(id);
        } catch (BusinessException e) {
            throw businessFault(e);
        } catch (RuntimeException e) {
            throw internalFault(e);
        }
    }

    @WebMethod(operationName = "listPendingRequests")
    @WebResult(name = "request")
    public List<ResourceRequestView> listPendingRequests() throws BusinessFault {
        try {
            return services.get().findPending();
        } catch (BusinessException e) {
            throw businessFault(e);
        } catch (RuntimeException e) {
            throw internalFault(e);
        }
    }

    private static BusinessFault businessFault(BusinessException e) {
        return new BusinessFault(e.getMessage(), new BusinessFaultInfo(e.getCode(), e.getMessage()), e);
    }

    private static BusinessFault internalFault(RuntimeException e) {
        String message = "No se pudo completar la operación";
        if (DependencyAvailability.isUnavailable(e)) {
            message = "El servicio no esta disponible temporalmente";
            return new BusinessFault(message,
                    new BusinessFaultInfo("SERVICE_UNAVAILABLE", message), e);
        }
        return new BusinessFault(message, new BusinessFaultInfo("INTERNAL_ERROR", message), e);
    }
}
