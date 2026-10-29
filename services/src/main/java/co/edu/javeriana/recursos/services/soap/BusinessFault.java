package co.edu.javeriana.recursos.services.soap;

import jakarta.xml.ws.WebFault;

/** Fault declarado en el WSDL, no una excepción técnica sin controlar. */
@WebFault(name = "BusinessFault", targetNamespace = ResourceRequestSoapService.NAMESPACE,
        faultBean = "co.edu.javeriana.recursos.services.soap.BusinessFaultInfo")
public class BusinessFault extends Exception {
    private final BusinessFaultInfo faultInfo;

    public BusinessFault(String message, BusinessFaultInfo faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    public BusinessFault(String message, BusinessFaultInfo faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    public BusinessFaultInfo getFaultInfo() {
        return faultInfo;
    }
}
