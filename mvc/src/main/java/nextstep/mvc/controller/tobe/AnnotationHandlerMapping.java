package nextstep.mvc.controller.tobe;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nextstep.mvc.HandlerMapping;
import nextstep.web.annotation.Controller;
import nextstep.web.annotation.RequestMapping;
import nextstep.web.support.RequestMethod;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnotationHandlerMapping implements HandlerMapping {

    private static final Logger log = LoggerFactory.getLogger(AnnotationHandlerMapping.class);

    private final Object[] basePackage;
    private final Map<HandlerKey, HandlerExecution> handlerExecutions;

    public AnnotationHandlerMapping(final Object... basePackage) {
        this.basePackage = basePackage;
        this.handlerExecutions = new HashMap<>();
    }

    public void initialize() {
        final Reflections reflections = new Reflections(basePackage);
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(Controller.class);

        for (Class<?> clazz : classes) {
            scanHandlers(clazz);
        }
        log.info("Initialized AnnotationHandlerMapping!");
    }

    private void scanHandlers(Class<?> clazz) {
        try {
            final List<Method> methods = Stream.of(clazz.getDeclaredMethods())
                    .filter(x -> x.isAnnotationPresent(RequestMapping.class))
                    .collect(Collectors.toList());
            addHandlerExecutions(clazz, methods);
        } catch (Exception e) {
            log.error("Exception : {}", e.getMessage(), e);
        }
    }

    private void addHandlerExecutions(Class<?> clazz, List<Method> methods) throws Exception {
        final Object instance = clazz.getDeclaredConstructor().newInstance();
        for (Method method : methods) {
            final HandlerKey handlerKey = HandlerKey.from(method);
            final HandlerExecution handlerExecution = new HandlerExecution(instance, method);
            handlerExecutions.put(handlerKey, handlerExecution);
        }
    }

    public Object getHandler(final HttpServletRequest request) {
        final String requestURI = request.getRequestURI();
        final RequestMethod method = RequestMethod.find(request.getMethod());
        final HandlerKey handlerKey = new HandlerKey(requestURI, method);
        return handlerExecutions.get(handlerKey);
    }
}
