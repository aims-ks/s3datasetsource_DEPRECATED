# S3 Dataset Source Plugin for [Unidata's THREDDS Project][1].

Provides a simple implementation of the [thredds DatasetSource interface][2].  
Allows datasets to reside in, and be accessed directly from S3 via the [THREDDS Data Server (TDS)][3].

## Configuration
Follow the documentation on the [thredds DatasetSource plugin page][2].  
`catalog.xml` - thredds catalog configuration.  
`threddsConfig.xml` - thredds  main configuration file.  
`docker-compose.yml` - docker compose file to start thredds TDS with supplied configuration.  

### `catalog.xml`

The `s3harvester` automatically creates a tree structure representing the NetCDF files found on S3.
THREDDS load its root catalogue using the `catalog.xml` file found at the root of this project.

To personalise the catalogue, modify the `catalog.xml` file to point it to the branches of interest
that have been harvested.

Example:
```
<?xml version="1.0" encoding="UTF-8"?>
<catalog name="eReefs model, derived Open Data Catalogue file"
         xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
         xmlns:xlink="http://www.w3.org/1999/xlink"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0 http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.1.0.6.xsd">

    <catalogRef name="S3 Bucket aims-ereefs-public-test" xlink:href="s3catalogue/aims-ereefs-public-test/catalog.xml" />
    <catalogRef name="Data derived from the eReefs GBR4 v2 model" xlink:href="s3catalogue/aims-ereefs-public-test/derived/ncaggregate/ereefs/gbr4_v2/ongoing/catalog.xml" />

</catalog>
```

### `threddsConfig.xml`

### `docker-compose.yml`

## Building
`$ mvn package` - build the plugin.  
Built artifact can be found in the target directory:  
`<project root>/target/s3datasetsource-1.0-SNAPSHOT-jar-with-dependencies.jar`

## Running
Store your AWS credentials in a file called `aws.env` in the root directory. You have to have a (free) AWS account to access this data.

`$ docker-compose up` - builds and starts the thredds TDS with the supplied configuration.

The data can then be seen at `http://localhost:8888/thredds/catalogue.html`

## Harvester

TODO
`http://localhost:8888/thredds/s3harvester`


### Credit  
[@jamesmcclain](https://github.com/jamesmcclain) original author of [S3RandomAccessFile][4].

[1]: https://github.com/Unidata/thredds
[2]: http://www.unidata.ucar.edu/software/thredds/current/tds/reference/DatasetSource.html
[3]: http://www.unidata.ucar.edu/software/thredds/current/tds/
[4]: https://github.com/Unidata/thredds/pull/832/files#diff-fd2b60e4477724acec18731154b8db0a

