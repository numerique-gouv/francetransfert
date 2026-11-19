FROM amazon/aws-cli:2.40.4

RUN yum install -y zip unzip findutils ca-certificates \
    && update-ca-trust \
    && yum clean all \
    && rm -rf /var/cache/yum /var/cache/dnf
