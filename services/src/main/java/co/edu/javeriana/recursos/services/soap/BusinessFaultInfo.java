package co.edu.javeriana.recursos.services.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/** Detalle público del error SOAP: código estable y mensaje de negocio. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BusinessFaultInfo", propOrder = {"code", "message"})
public class BusinessFaultInfo {
    private String code;
    private String message;

    public BusinessFaultInfo() {
    }

    public BusinessFaultInfo(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
