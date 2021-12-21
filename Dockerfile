FROM unidata/thredds-docker:4.6.19
COPY go.sh /go.sh
COPY --chown=1000:1000 robots.txt /usr/local/tomcat/webapps/ROOT/
RUN chmod a+x /go.sh
ENTRYPOINT [""]
CMD ["/go.sh"]
