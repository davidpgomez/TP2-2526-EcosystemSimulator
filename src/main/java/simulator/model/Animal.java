package simulator.model;

import org.json.JSONObject;

import simulator.misc.Utils;
import simulator.misc.Vector2D;

public abstract class Animal implements Entity, AnimalInfo {

    protected String geneticCode;
    protected Diet diet;
    protected State state;
    protected Vector2D pos;
    protected Vector2D dest; //destino del animal
    protected double energy; //Cuando llega a 0, el animal muere
    protected double speed;
    protected double age;
    protected double desire; //deseo del animal, que cambia durante la simulación.
    protected double sightRange; //el radio del campo visual del animal 
    protected Animal mateTarget; //el radio del campo visual del animal (para decidir qué animales puede ver).
    protected Animal baby; //una referencia que indica si el animal lleva un bebé que no ha nacido aún.
    protected AnimalMapView regionMngr; //gestor de regiones para poder consultar información o hacer operaciones
    protected SelectionStrategy mateStrategy; // estrategia de selección para buscar pareja.

    //CONSTRUCTORAS
    //Constructora 1 (Crear los objetos iniciales a partir de parámetros específicos)
    protected Animal(String geneticCode, Diet diet, double sightRange, double initSpeed, SelectionStrategy mateStrategy, Vector2D pos) {
        if (geneticCode == null || geneticCode.isBlank()) {
            throw new IllegalArgumentException("geneticCode no puede estar vacio o ser nulo");
        }
        if (sightRange <= 0) {
            throw new IllegalArgumentException("sightRange tiene que ser > 0");
        }
        if (initSpeed <= 0) {
            throw new IllegalArgumentException("initSpeed tiene que ser > 0");
        }
        if (mateStrategy == null) {
            throw new IllegalArgumentException("mateStrategy no puede ser nulo");
        }
        this.geneticCode = geneticCode;
        this.diet = diet;
        this.sightRange = sightRange;
        this.pos = pos;
        this.mateStrategy = mateStrategy;
        this.speed = Utils.getRandomizedParameter(initSpeed, 0.1);
        this.state = State.NORMAL;
        this.energy = 100.0;
        this.desire = 0.0;
        this.dest = null;
        this.mateTarget = null;
        this.baby = null;
        this.regionMngr = null;
        this.age = 0.0;
    }

    //Constructora 2 (Crear un nuevo animal a partir de dos padres, con características heredadas y variación aleatoria)
    protected Animal(Animal p1, Animal p2) {
        this.dest = null;
        this.baby = null;
        this.mateTarget = null;
        this.regionMngr = null;
        this.state = State.NORMAL;
        this.desire = 0.0;
        this.geneticCode = p1.geneticCode;
        this.diet = p1.diet;
        this.mateStrategy = p2.mateStrategy;
        this.energy = (p1.energy + p2.energy) / 2; //la energia es la media de ambos padres
        this.pos = p1.getPosition().plus(Vector2D.get_random_vector(-1, 1).scale(60.0 * (Utils.RAND.nextGaussian() + 1))); //la posición inicial del bebé es cercana a la del padre 1, con una variación aleatoria
        this.sightRange = Utils.getRandomizedParameter((p1.getSightRange() + p2.getSightRange()) / 2.0, 0.2); //el rango de visión es la media de ambos padres
        this.speed = Utils.getRandomizedParameter((p1.getSpeed() + p2.getSpeed()) / 2.0, 0.2); //la velocidad es la media de ambos padres
        this.age = 0.0; //la edad inicial es 0
    }

    //el gestor de regiones invocará a este método al añadir el animal a la simulación:
    public void init(AnimalMapView regionMngr) {
        this.regionMngr = regionMngr;
        if (pos == null) { //si no se ha proporcionado una posición inicial, se asigna una posición aleatoria dentro del mapa
            double x = Utils.RAND.nextDouble(regionMngr.getWidth());
            double y = Utils.RAND.nextDouble(regionMngr.getHeight());
            pos = new Vector2D(x, y);
        } else { // sino, se ajusta la posición proporcionada para que esté dentro de los límites del mapa
            adjustPosition();
        }
        
        // Calculamos un destino inicial aleatorio para el animal dentro del mapa
        double destX = Utils.RAND.nextDouble(regionMngr.getWidth());
        double destY = Utils.RAND.nextDouble(regionMngr.getHeight());
        dest = new Vector2D(destX, destY);
    }

    public Animal deliverBaby() { //El simulador invocará a este método para que nazcan los animales.
        Animal b = baby;
        baby = null;
        return b;
    }

    protected void move(double speed) { // las subclases usan este método para actualizar la posición del animal 
        pos = pos.plus(dest.minus(pos).direction().scale(speed));
    }

    protected void setState(State state) { //llama a un método correspondiente, dependiendo del estado, para llevar a cabo alguna accion complementaria.
        this.state = state;
        switch (state) {
            case NORMAL:
                setNormalStateAction();
                break;
            case HUNGER:
                setHungerStateAction();
                break;
            case DANGER:
                setDangerStateAction();
                break;
            case MATE:
                setMateStateAction();
                break;
        }
    }

    //Métodos abstractos para que cada subclase implemente las acciones específicas de cada estado.
    protected abstract void setNormalStateAction();

    protected abstract void setMateStateAction();

    protected abstract void setHungerStateAction();

    protected abstract void setDangerStateAction();

    protected abstract void setDeadStateAction();

    //Metodos para ajustar la posición del animal dentro de los límites del mapa, y para comprobar si está fuera del mapa (en cuyo caso se ajustará su posición o se considerará que ha muerto, dependiendo de la implementación concreta de cada subclase).
    protected void adjustPosition() {
        double x = pos.getX();
        double y = pos.getY();
        int width = regionMngr.getWidth();
        int height = regionMngr.getHeight();
        while (x >= width) {
            x = x - width;
        }
        while (x < 0) {
            x = x + width;
        }
        while (y >= height) {
            y = y - height;
        }
        while (y < 0) {
            y = y + height;
        }
        pos = new Vector2D(x, y);
    }

    protected boolean isOutOfMap() {
        double x = pos.getX();
        double y = pos.getY();
        return x < 0 || x >= regionMngr.getWidth() || y < 0 || y >= regionMngr.getHeight();
    }

    // -------- METODOS DE ANIMALINFO.JAVA -----------
    @Override
    public void update(double dt) {
        if (energy == 0) {
            state = State.DEAD;
        } else {
            energy--;
            age--;
        }
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public Vector2D getPosition() {
        return pos;
    }

    @Override
    public String getGeneticCode() {
        return geneticCode;
    }

    @Override
    public Diet getDiet() {
        return diet;
    }

    @Override
    public double getSpeed() {
        return speed;
    }

    @Override
    public double getSightRange() {
        return sightRange;
    }

    @Override
    public double getEnergy() {
        return energy;
    }

    @Override
    public double getAge() {
        return age;
    }

    @Override
    public Vector2D getDestination() {
        return dest;
    }

    @Override
    public boolean isPregnant() {
        return baby != null;
    }

    @Override
    public JSONObject asJSON() {
        JSONObject json = new JSONObject();
        json.put("pos", pos.asJSONArray());
        json.put("gcode", geneticCode);
        json.put("diet", diet.name());
        json.put("state", state.name());
        return json;
    }

}
