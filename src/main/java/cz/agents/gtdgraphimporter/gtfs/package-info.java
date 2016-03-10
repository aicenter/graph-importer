/**
 * 
 * GTFS importer to create PT planning graph from timetables 
 * 
 * 
 * It is composed of few components.
 * {@link cz.agents.gtdgraphimporter.gtfs.GTFSImporter} is
 * the main class used for the transformations to appropriate formats. The 
 * importer requires an instance of 
 * {@link cz.agents.gtdgraphimporter.gtfs.GTFSDataLoader}
 * which provides the input for the importer. E.g. 
 * {@link cz.agents.gtdgraphimporter.gtfs.GTFSDatabaseLoader}
 * provides timetables stored in a database. The implementation of data 
 * loaders was inspired by {@link javax.xml.parsers.SAXParser} in the way that 
 * loaders generates events (method calls) which are passed to a provided 
 * {@link cz.agents.gtdgraphimporter.gtfs.GTFSDataHandler}
 * instance. In fact, the importer creates appropriate data handlers which are 
 * able to construct representations of timetables in proper formats and then 
 * it returns these representations.
 * 
 * The 
 * {@link cz.agents.gtdgraphimporter.gtfs.GTFSDatabaseLoader}
 * is able to prune timetables using given interval. This feature allows 
 * speeding up (and reducing memory consumption) of the import but it should 
 * be used rarely. Better way to implement this filtering is placing a filter 
 * between the data loader and the importer.
 */

package cz.agents.gtdgraphimporter.gtfs;

