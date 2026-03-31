package ru.managerfix.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an event handler. Method must have a single parameter of type
 * ManagerFixEvent or a subclass. Priority defaults to NORMAL.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MFEventHandler {

    EventPriority priority() default EventPriority.NORMAL;
}
