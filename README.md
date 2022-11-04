# ElectricityCostDashboard

This is a web app visualizing the current electricity prices and sources, inspired by exceptionally high electricity prices in 2022. In addition it also features an electricity cost calculator with which you can determine how much your electricity consumption in Finland has cost based on real Fingrid datahub exported data.
Built using [Vaadin](https://vaadin.com/) & [Spring Boot](https://spring.io/). A hosted version is available at [sahko.vesanieminen.com](http://sahko.vesanieminen.com).

## Visualizer
Inspired by sahko.tk. Added my own twist to it with Fingrid public data to see e.g. the current wind production and production estimate in order to see its correlation to the price.

<img width="910" alt="Screenshot 2022-11-01 at 22 53 53" src="https://user-images.githubusercontent.com/108755/199339058-91df53c0-cca2-4185-9a28-850b05004b7c.png">

## Calculator
With this you can calculate the true costs of your electricity sale contract. Also you can compare the market spot price to a fixed on in order to see which one would have been the cheaper option for you.

![Screenshot 2022-10-28 at 21 34 32](https://user-images.githubusercontent.com/108755/199401516-603172ab-e833-43c9-9d40-e51bbcb861e0.png)

## List
The UI is inspired by Fingrid Tuntihinta app. (My wife hates graphs so I built this for her 😉)

<img width="496" alt="Screenshot 2022-11-02 at 6 55 51" src="https://user-images.githubusercontent.com/108755/199401633-098ef27b-c135-4580-a972-ef791528878e.png">

## Running the application

The project is a standard Maven project. To run it from the command line,
type `mvnw` (Windows), or `./mvnw` (Mac & Linux), then open
http://localhost:8080 in your browser.

You can also import the project to your IDE of choice as you would with any
Maven project. Read more on [how to import Vaadin projects to different 
IDEs](https://vaadin.com/docs/latest/flow/guide/step-by-step/importing) (Eclipse, IntelliJ IDEA, NetBeans, and VS Code).

## Deploying to Production

To create a production build, call `mvnw clean package -Pproduction` (Windows),
or `./mvnw clean package -Pproduction` (Mac & Linux).
This will build a JAR file with all the dependencies and front-end resources,
ready to be deployed. The file can be found in the `target` folder after the build completes.

Once the JAR file is built, you can run it using
`java -jar target/froniusvizualizer-1.0-SNAPSHOT.jar`

## Project structure

- `MainLayout.java` in `src/main/java` contains the navigation setup (i.e., the
  side/top bar and the main menu). This setup uses
  [App Layout](https://vaadin.com/components/vaadin-app-layout).
- `views` package in `src/main/java` contains the server-side Java views of your application.
- `views` folder in `frontend/` contains the client-side JavaScript views of your application.
- `themes` folder in `frontend/` contains the custom CSS styles.

## Useful links

- Read the documentation at [vaadin.com/docs](https://vaadin.com/docs).
- Follow the tutorials at [vaadin.com/tutorials](https://vaadin.com/tutorials).
- Watch training videos and get certified at [vaadin.com/learn/training](https://vaadin.com/learn/training).
- Create new projects at [start.vaadin.com](https://start.vaadin.com/).
- Search UI components and their usage examples at [vaadin.com/components](https://vaadin.com/components).
- View use case applications that demonstrate Vaadin capabilities at [vaadin.com/examples-and-demos](https://vaadin.com/examples-and-demos).
- Discover Vaadin's set of CSS utility classes that enable building any UI without custom CSS in the [docs](https://vaadin.com/docs/latest/ds/foundation/utility-classes). 
- Find a collection of solutions to common use cases in [Vaadin Cookbook](https://cookbook.vaadin.com/).
- Find Add-ons at [vaadin.com/directory](https://vaadin.com/directory).
- Ask questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/vaadin) or join our [Discord channel](https://discord.gg/MYFq5RTbBn).
- Report issues, create pull requests in [GitHub](https://github.com/vaadin/platform).
