package co.edu.javeriana.recursos.services.client;

import co.edu.javeriana.recursos.contracts.ResourceRequestService;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServicesWarmupTest {
    @Test void callsLogicWhenStartingServices() {
        ResourceRequestService remote = mock(ResourceRequestService.class);
        when(remote.findAll()).thenReturn(List.of());
        ServicesWarmup bean = new ServicesWarmup();
        assertNotNull(ServicesWarmup.class.getAnnotation(Singleton.class));
        assertNotNull(ServicesWarmup.class.getAnnotation(Startup.class));
        assertDoesNotThrow(() -> bean.warmUp(() -> remote));
        verify(remote).findAll();
    }

    @Test void failureDoesNotPreventDeployment() {
        ServicesWarmup bean = new ServicesWarmup();
        assertDoesNotThrow(() -> bean.warmUp(() -> {
            throw new IllegalStateException("Logic caido");
        }));
    }
}
