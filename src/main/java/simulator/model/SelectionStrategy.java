package simulator.model;

import java.util.List;

public interface SelectionStrategy {
	Animal select(Animal a, List<Animal> as); // Animal no hace falta importarlo porque está en el mismo paquete
}