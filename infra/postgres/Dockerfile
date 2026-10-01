FROM postgres:16-alpine
# Keep the upstream entrypoint and database version. Alpine's tiny native
# su-exec provides the same user/group + exec interface used by the entrypoint,
# without shipping the old Go runtime bundled in upstream's gosu helper.
RUN apk upgrade --no-cache \
    && apk add --no-cache su-exec \
    && ln -sf /sbin/su-exec /usr/local/bin/gosu
