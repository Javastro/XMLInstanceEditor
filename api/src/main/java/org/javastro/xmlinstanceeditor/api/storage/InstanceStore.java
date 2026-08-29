package org.javastro.xmlinstanceeditor.api.storage;

import java.util.List;
import java.util.Optional;

public interface InstanceStore<TState> {
    EditedInstance<TState> save(EditedInstance<TState> instance);

    Optional<EditedInstance<TState>> findById(String id);

    List<EditedInstance<TState>> findAll();

    boolean delete(String id);
}
