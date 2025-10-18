package spingcloud.diplom.service.logging;


import org.slf4j.Logger;

public class LoggingUtil {

    private LoggingUtil() {

    }

    public static void logMethodEntry(Logger logger, String methodName, Object... params) {
        if (logger.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder("Entering method: ").append(methodName);
            if (params.length > 0) {
                sb.append(" with parameters: ");
                for (int i = 0; i < params.length; i++) {
                    sb.append("param").append(i).append("=");
                    if (params[i] != null) {
                        // Маскируем пароли и токены
                        if (params[i] instanceof String &&
                                (((String) params[i]).contains("password") ||
                                        ((String) params[i]).contains("token"))) {
                            sb.append("***");
                        } else {
                            sb.append(params[i]);
                        }
                    } else {
                        sb.append("null");
                    }
                    if (i < params.length - 1) {
                        sb.append(", ");
                    }
                }
            }
            logger.debug(sb.toString());
        }
    }

    public static void logMethodExit(Logger logger, String methodName, Object result) {
        if (logger.isDebugEnabled()) {
            String message = "Exiting method: " + methodName;
            if (result != null) {
                // Маскируем чувствительные данные
                String resultStr = result.toString();
                if (resultStr.contains("auth-token") || resultStr.contains("password")) {
                    message += " with result: ***";
                } else {
                    message += " with result: " + result;
                }
            }
            logger.debug(message);
        }
    }

    public static void logError(Logger logger, String methodName, Exception e, String message) {
        logger.error("Error in method {}: {} - {}", methodName, message, e.getMessage(), e);
    }

    public static void logPerformance(Logger logger, String operation, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        if (duration > 1000) { // Логируем только медленные операции
            logger.warn("Slow operation detected: {} took {} ms", operation, duration);
        } else if (logger.isDebugEnabled()) {
            logger.debug("Operation {} completed in {} ms", operation, duration);
        }
    }
}