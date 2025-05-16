# DEPRECATED

This project is Deprecated. THREDDS 5 now has native support for AWS S3.

# S3 Dataset Source Plugin for [Unidata's THREDDS Project][1].

This project provides a simple implementation of the [thredds DatasetSource interface][2].  
It allows datasets to reside in, and be accessed directly from S3 via the [THREDDS Data Server (TDS)][3].

## Thomas Powell's S3DatasetSource plugin

THREDDS do not have S3 capabilities. [Thomas Powell](https://github.com/tpowellmeto) developed a plugin, [S3DatasetSource][4], to add that capability.
It can be used to manually link some NetCDF files from S3 to a THREDDS server.

Note that the setup instructions in the `README.md` file are lacking some steps.
For example, the S3 buckets `mogreps-g` and `mogreps-uk` are hardcoded in the `go.sh` file.

The project creates a Docker container containing THREDDS and the S3DatasetSource plugin.
We strongly suggest using the Docker container since it prevents issues with library versions
and makes it much easier to maintain. It also takes care of setting up the S3 plugin in THREDDS.

Adding S3 capability to an existing THREDDS setup is a lot more complex. There are no instructions describing the steps.
The installation is defined in `Dockerfile`, `go.sh`, `ingest.py` and probably a few more files.
It's more complex that is sounds. Use the Docker container if you can.

## This S3DatasetSource branch

The eReefs system periodically adds more NetCDF files to our S3 buckets, so it needs an extra functionality
to be able to automatically refresh (aka harvest) the list of available NetCDF files from S3.
The harvest process can be triggered periodically by a crontab entry.

## Configuration

List of configuration files:
- `catalog.xml` - THREDDS catalog configuration.
- `s3harvester.xml` - harvester of S3 buckets. See *S3 Harvester* section bellow for more information.
- `threddsConfig.xml` - THREDDS  main configuration file.
- `docker-compose.yml` - docker compose file to start THREDDS TDS with supplied configuration.

Documentation about the DatasetSource plugin support in THREDDS: [thredds DatasetSource plugin page][2].  

### `catalog.xml`

The `s3harvester` automatically creates a tree structure representing the NetCDF files found on S3.
THREDDS load its root catalogue using the `catalog.xml` file found at the root of this project.

You will need to modify the `catalog.xml` file found at the root of this project to
to refer to harvested `catalog.xml` files of interest, found in `s3catalogue` directory.

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

Set the Google analytics key, if desired.

```
<htmlSetup>
    <googleTrackingCode></googleTrackingCode>
</htmlSetup>
```

### `docker-compose.yml`

THREDDS is set to run on port `8888`. Change the port in that configuration file if required.
```
ports:
  - '8888:8080'    
```

## Private S3 bucket support

The original plugin have some support for connect to private S3 buckets. There is very little documentation
about it and it's not required for our usecase, so we decided to drop this feature.

> Store your AWS credentials in a file called `aws.env` in the root directory. You have to have a (free) AWS account to access this data.

## Building

`$ mvn clean package` - build the plugin.

Built artifact can be found in the target directory:  
`<project root>/target/s3datasetsource-1.0-SNAPSHOT-jar-with-dependencies.jar`

## Running

`$ docker-compose up` - builds and starts the thredds TDS with the supplied configuration.

**NOTE:** You will need to build the plugin prior to run this.

The data can then be seen at `http://localhost:8888/thredds/catalog.html`

## S3 Harvester

The `s3harvester` service was added to this plugin to keep the catalogue up to date. 

It replace the content of the `s3catalogue` directory with current NetCDF files
found in the configured S3 buckets.

**Algorithm**
1. Listen to requests sent to `/thredds/s3harvester`
2. When a request is received, it loads the list of files found in the configured S3 buckets (see `s3harvester.xml` file)
3. It filters the list according to file extension (`.nc`) and to the configured `<paths>`
4. It replicates the directory tree structure in a temporary directory, and create the necessary `catalog.xml` files
5. If the harvesting completed successfully, it overwrites the content of the `s3catalogue` directory with the content of the temporary directory
6. Then, it reloads THREDDS webapp configuration

### Configure the S3 Harvester

The S3 Harvester configuration file `s3harvester.xml` is found at the root of this project.

#### Root element `<s3HarvesterConfig>`

| Element                | Necessity | Cardinality | Description |
| ---------------------- | --------- | ----------- | ----------- |
| `<catalogueDirectory>` | Optional  | 1           | Local path on the filesystem where the S3 configuration structure will be created. Default: `/usr/local/tomcat/content/thredds/s3catalogue` |
| `<buckets>`            | Mandatory | 1           | Element used to group `<bucket>` together. |

**Notes**

- The `<catalogueDirectory>` must be accessible by the THREDDS catalogue loading mechanism.
    Only set this element if you are experiencing issues.

#### Element `<buckets>`

| Element                | Necessity | Cardinality | Description |
| ---------------------- | --------- | ----------- | ----------- |
| `<bucket>`             | Mandatory | 1+          | Configuration for a S3 bucket. |

#### Element `<bucket>`

| Attribute              | Necessity | Description |
| ---------------------- | --------- | ----------- |
| `name`                 | Mandatory | The name of the S3 bucket to harvest. |

| Element                | Necessity | Cardinality | Description |
| ---------------------- | --------- | ----------- | ----------- |
| `<paths>`              | Optional  | 1           | Element used to group `<path>` together. |

#### Element `<paths>`

| Element                | Necessity | Cardinality | Description |
| ---------------------- | --------- | ----------- | ----------- |
| `<path>`               | Mandatory | 1+          | S3 path of the S3 bucket to harvest. |

**Notes**

- The element `<paths>` of a `<bucket>` is **optional**. If not present, the whole bucket is harvested.
- The element `<path>` is **recursive**. Every NetCDF files found in the specified path will be listed
    in the catalogue, and every NetCDF files found in sub directories, and so on.
- The directory tree structure of the S3 bucket is replicated in the catalogue structure,
    for specified paths. This can lead to long THREDDS URL when harvesting a single branch
    deep in the S3 bucket.  
    **Harvesting**
    ```
    <buckets>
        <bucket name="my-bucket">
            <paths>
                <path>deep/branch/deep/within/the/s3/bucket</path>
            </paths>
        </bucket>
    <buckets>
    ```
    produces URLs like this in THREDDS:
    ```
    https://domain.com/thredds/s3catalogue/my-bucket/deep/branch/deep/within/the/s3/bucket/file.nc
    ```

**`s3harvester.xml` example**
```
<?xml version="1.0" encoding="UTF-8"?>
<s3HarvesterConfig>
    <buckets>
        <bucket name="aims-ereefs-public-test">
            <paths>
                <path>derived/ncaggregate</path>
                <path>derived/ncanimate</path>
            </paths>
        </bucket>
        <bucket name="my-bucket" />
    </buckets>
</s3HarvesterConfig>
```

### Manually run the S3 Harvester

The S3 Harvester service can be run manually with this URL:  
`http://localhost:8888/thredds/s3harvester`

If the harvesting succeed, the system returns HTTP code 200 with the following message:
`Harvesting done`

If something goes wrong, the system returns an error HTTP code and some logs are
displayed in docker console.

### Automatically run the S3 Harvester

The intention of the S3 Harvester is to be automatically called on regular basis with a
service like `crontab`.

`$ crontab -e`

```
# Run once a day at midnight
0 0 * * * curl --silent "http://localhost:8888/thredds/s3harvester"
```

## Credit  
[@tpowellmeto](https://github.com/tpowellmeto) original author of the [s3datasetsource][4] plugin.  
[@jamesmcclain](https://github.com/jamesmcclain) original author of [S3RandomAccessFile][5] support in THREDDS.

[1]: https://github.com/Unidata/thredds
[2]: http://www.unidata.ucar.edu/software/thredds/current/tds/reference/DatasetSource.html
[3]: http://www.unidata.ucar.edu/software/thredds/current/tds/
[4]: https://github.com/informatics-lab/s3datasetsource
[5]: https://github.com/Unidata/thredds/pull/832/files#diff-fd2b60e4477724acec18731154b8db0a
