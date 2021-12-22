# THREDDS Docker container

## TODO

1. Secure the S3HarvesterController (will be called by cron on Host)
2. Add support for `Size` and `Last Modified` in the catalogue

## Doc

Documentation about how to create an updated docker image for THREDDS with S3 support, configured for eReefs.

1. Download the S3 THREDDS plugin:

    Original:

    https://github.com/informatics-lab/s3datasetsource

    ```
    $ git clone https://github.com/informatics-lab/s3datasetsource.git
    ```

    AIMS fork:

    https://github.com/aims-ks/s3datasetsource

    ```
    $ git clone https://github.com/aims-ks/s3datasetsource.git
    ```

2. Fix bugs (if cloning from original)

    **src/main/java/uk/co/informaticslab/Constants.java**

    Fix hardcodded region ID to the standard S3 region:

    Change
    ```
    public static final Regions MY_S3_DATA_REGION = Regions.EU_WEST_2;
    ```
    to
    ```
    public static final Regions MY_S3_DATA_REGION = Regions.US_EAST_1;
    ```

3. Configure (if cloning from original)

    ```
    catalog.xml - thredds catalog configuration.
    threddsConfig.xml - thredds main configuration file.
    docker-compose.yml - docker compose file to start thredds TDS with supplied configuration.
    ```

    **catalog.xml**

    There is no documentation, no example about how to setup a S3 catalogue.

    ```
    https://<BUCKET>.s3-<REGION>.amazonaws.com/<PATH>
    https://aims-ereefs-public-test.s3-ap-southeast-2.amazonaws.com/derived/ncaggregate/ereefs/gbr4_bgc_924/ongoing/all-one/gbr4_bgc_924-all-one.nc
    ```

    Modify go.sh

    go.sh calls `ingest.py <S3 BUCKET ID> <S3 BUCKET ID> ...`
    ```
    /usr/bin/env python3 /usr/local/src/ingest.py mogreps-g mogreps-uk
    ```
    to
    ```
    /usr/bin/env python3 /usr/local/src/ingest.py aims-ereefs-public-test
    ```

    Copy resources/aims-ereefs-public-test.jinja to src/templates

    **docker-compose.yml**

    For development purpose, lets change the port to a free port.

    Change:
    ```
    ports:
      - '80:8080'    
    ```
    to
    ```
    ports:
      - '8888:8080'    
    ```

    Mount files / directory to the docker image

    Change:
    ```
    volumes:
      - ./threddsConfig.xml:/usr/local/tomcat/content/thredds/threddsConfig.xml
      - ./target/s3datasetsource-1.0-SNAPSHOT-jar-with-dependencies.jar:/usr/local/tomcat/webapps/thredds/WEB-INF/lib/s3datasetsource-1.0-SNAPSHOT-jar-with-dependencies.jar
    ```
    to
    ```
    volumes:
      - ./threddsConfig.xml:/usr/local/tomcat/content/thredds/threddsConfig.xml
      - ./target/s3datasetsource-1.0-SNAPSHOT-jar-with-dependencies.jar:/usr/local/tomcat/webapps/thredds/WEB-INF/lib/s3datasetsource-1.0-SNAPSHOT-jar-with-dependencies.jar
      - ./catalog.xml:/usr/local/tomcat/content/thredds/catalog.xml
      - ./s3:/usr/local/tomcat/content/thredds/s3
    ```

    **threddsConfig.xml**

    Allow WMS service

    Change
    ```
    <!--
    <WMS>
      <allow>false</allow>
      <allowRemote>false</allowRemote>
      <maxImageWidth>2048</maxImageWidth>
      <maxImageHeight>2048</maxImageHeight>
    </WMS>
    -->
    ```
    to
    ```
    <WMS>
      <allow>true</allow>
      <allowRemote>true</allowRemote>
      <maxImageWidth>2048</maxImageWidth>
      <maxImageHeight>2048</maxImageHeight>
    </WMS>
    ```

    TODO Set the Google analytics key
    
    Change:
    ```
    <htmlSetup>
        <googleTrackingCode></googleTrackingCode>
    </htmlSetup>
    ```
    to
    ```
    <htmlSetup>
        <googleTrackingCode>   TODO   </googleTrackingCode>
    </htmlSetup>
    ```

    **DockerFile**

    THREDDS 4.6.10 uses the old Debian repository `jessie-backports`.
    We need to use a more recent version to be able to compile it.

    If you see this error:
    ```
    Err http://deb.debian.org jessie-backports/main amd64 Packages
      404  Not Found
    ```

    Change:
    ```
    FROM unidata/thredds-docker:4.6.10
    ```
    to
    ```
    FROM unidata/thredds-docker:4.6.19
    ```

4. Create the docker container

    Clean up?
    ```
    $ docker container prune --force
    $ docker image prune --force
    ```

    Compile the plugin and create the docker container

    ```
    $ cd s3datasetsource
    $ mvn clean package
    $ docker-compose up
    ```

    If you get **ERROR: Couldn't connect to Docker daemon**,
    check if your user is member of the *docker* group:
    ```
    $ groups
    ```

    If not, add your user to the *docker* group. You may need to log out / log in
    to your desktop environment for the changes to be effective.
    ```
    $ sudo usermod -a -G docker <YOUR USERNAME>
    ```

    **NOTE:** The maven command can be done within your IDE.
    It can also be automatically done in the Docker container,
    but Docker needs to download the dependencies every time, which adds
    about 10 minutes to the deployment time. It's much quicker to do it locally.
    
    If you modify the configuration, you will have to force a new compile with:
    ```
    $ docker-compose up --force-recreate --build
    ```

5. Verify and connect to the running docker instance for debugging

    You can connect to the Docker container to verify what was done.

    Connect to the docker image
    ```
    $ docker ps
    $ docker exec -it thredds bash
    ```

    Enable colours with ls and create "ll" alias
    ```
    # alias ls='ls --color'; alias ll='ls -l'
    ```

    Install packages for `ps` and `sudo` (very useful for debugging)
    ```
    # apt-get update && apt-get install procps sudo
    ```

    Check generated files
    ```
    # vi /usr/local/tomcat/content/thredds/catalog.xml
    # vi /usr/local/tomcat/content/thredds/threddsConfig.xml
    ```

    Disable vi Visual mode

    **NOTE:** Some version of `vi` have `visual mode` enabled by default.
    That makes mouse selection and quick copy / paste impossible.
    It might have some advantages, but I could not find any.
    It's just an obnoxious feature in my opinion.
    ```
    :set mouse-=a
    ```

    You won't be able to copy paste this command because the `visual mode`
    is enable by default. You will have to manually type it.
    Be careful with the syntax; there is a `-` (minus sign) before the `=` (equal sign).

    To disable it permanently:
    ```
    $ sudo vim /usr/share/vim/vim(version)/defaults.vim
    Find the command "set mouse=a" and comment it
    "if has('mouse')
    "  set mouse=a
    "endif
    ```

6. Visit URL:

    http://localhost:8888/thredds/catalog.html

    **NOTE:** In the original project, `ingest.py` was used to generate the catalog.xml file.
    THREDDS doesn't have support for generic. Needs to be re-written

7. Harvest S3 bucket(s)

    I have created a REST endpoint which automatically harvest the
    S3 bucket(s) and restart THREDDS webapp.

    http://localhost:8888/thredds/s3harvester

    This endpoint should be periodically called by the cron.
    
    **NOTE:** It's currently a public API. It's susceptible to DDOS attack and should be protected.

    Suggestion for protecting the endpoint:
    - Restrict with username / password
    - Restrict according to request provenance (only allow when called from localhost)
    - Restrict frequency (only perform harvest if last harvest was performed more than X minutes ago)

--------------------

## Old stuff

1. Download the THREDDS Docker image:

    https://github.com/Unidata/thredds-docker

    ```
    $ docker run -d -p 8888:8080 unidata/thredds-docker
    ```
    Visit: http://localhost:8888/thredds/catalog.html

    > Compile ?:
    > ```
    > $ git clone https://github.com/Unidata/thredds-docker.git
    > $ cd thredds-docker
    > $ docker-compose ???
    > ```

2. Download & compile the S3 plugin:
    https://github.com/informatics-lab/s3datasetsource

    ```
    $ git clone https://github.com/informatics-lab/s3datasetsource.git
    $ cd s3datasetsource
    $ mvn clean package
    ```

3. Install the S3 plugin in THREDDS within the docker image

    Connect to the docker image
    ```
    $ docker ps
    $ docker cp s3datasetsource/target/s3datasetsource-1.0-SNAPSHOT-jar-with-dependencies.jar <DOCKER ID>:/usr/local/tomcat/webapps/thredds/WEB-INF/lib/s3datasetsource-1.0-SNAPSHOT-jar-with-dependencies.jar
    $ docker exec -it <DOCKER CONTAINER ID> bash
    # alias ls='ls --color'; alias ll='ls -l'
    # apt-get update && apt-get install procps sudo
    # vi /usr/local/tomcat/content/thredds/threddsConfig.xml
    ```

    Disable vi Visual mode
    ```
    :set mouse-=a
    ```

    ```
    <threddsConfig>
        ...
        <!--
        Add a DataSource - essentially an IOSP with access to Servlet request parameters
        <datasetSource>my.package.DatsetSourceImpl</datasetSource>
        -->
        <datasetSource>uk.co.informaticslab.S3DatasetSource</datasetSource>
        ...
    </threddsConfig>
    ```

    https://stackoverflow.com/questions/22907231/copying-files-from-host-to-docker-container

4. Configure the plugin

    Add this to the Dockerfile to copy the catalog.xml file to THREDDS
    ```
    COPY catalog.xml /usr/local/tomcat/content/thredds/catalog.xml
    ```

5. Upload the docker image to AWS ECR

6. Setup AWS ECS to spawn a THREDDS server with the ECR image when there is no server running (reliability)
