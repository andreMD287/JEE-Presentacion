package co.edu.javeriana.recursos.logic.ejb;

import co.edu.javeriana.recursos.logic.port.ResourceRequestRepository;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogicWarmupTest {
    @Test void warmsUpRemoteRepositoryAtStartup() {
        ResourceRequestRepository repository = mock(ResourceRequestRepository.class);
        when(repository.findAll()).thenReturn(List.of());
        LogicWarmup bean = new LogicWarmup();
        bean.repository = repository;
        assertNotNull(LogicWarmup.class.getAnnotation(Startup.class));
        assertNotNull(LogicWarmup.class.getAnnotation(Singleton.class));
        assertDoesNotThrow(bean::warmUp);
        verify(repository).findAll();
    }

    @Test void failureDoesNotPreventDeployment() {
        ResourceRequestRepository repository = mock(ResourceRequestRepository.class);
        when(repository.findAll()).thenThrow(new IllegalStateException("Data caido"));
        LogicWarmup bean = new LogicWarmup();
        bean.repository = repository;
        assertDoesNotThrow(bean::warmUp);
    }
}
