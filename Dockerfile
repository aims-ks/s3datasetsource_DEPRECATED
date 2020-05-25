FROM unidata/thredds-docker:4.6.14
COPY go.sh /go.sh
COPY --chown=tomcat:tomcat robots.txt /usr/local/tomcat/webapps/ROOT/
RUN chmod a+x /go.sh
ENTRYPOINT [""]
CMD ["/go.sh"]
