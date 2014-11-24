package org.jboss.pnc.core.builder;

import org.jboss.pnc.model.BuildStatus;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Created by <a href="mailto:matejonnet@gmail.com">Matej Lazar</a> on 2014-11-23.
 */
class Task<T> {
    private T task;

    private Status status;

    Task(T task) {
        this.task = task;
        status = Status.NEW;
    }

    T getTask() {
        return task;
    }

    void setBuilding() {
        status = Status.BUILDING;
    }

    void completedSuccessfully() {
        status = Status.SUCCESS;
    }

    void completedWithError() {
        status = Status.FAILED;
    }

    boolean isNew() {
        return Status.NEW.equals(status);
    }

    boolean isBuilding() {
        return Status.BUILDING.equals(status);
    }

    boolean isDone() {
        return Status.SUCCESS.equals(status) || Status.FAILED.equals(status);
    }

    boolean hasResolvedDependencies(Set<Task<T>> tasks) {
        Predicate<Task> filterSuccess = t -> t.status.equals(BuildStatus.SUCCESS);
        long successfullyCompleted = tasks.stream().filter(filterSuccess).count();
        return successfullyCompleted == tasks.size();
    }

    enum Status {
        NEW, BUILDING, SUCCESS, FAILED;
    }
}
