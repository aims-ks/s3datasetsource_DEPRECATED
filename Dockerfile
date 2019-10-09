FROM unidata/thredds-docker:4.6.14
COPY go.sh /go.sh
ENTRYPOINT [""]
CMD ["/go.sh"]
