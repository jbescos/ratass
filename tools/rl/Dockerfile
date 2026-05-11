FROM maven:3.9.9-eclipse-temurin-21

ENV DEBIAN_FRONTEND=noninteractive
ENV PYTHON_BIN=/opt/ratass-rl-venv/bin/python
ENV PIP_DISABLE_PIP_VERSION_CHECK=1
ENV PYTHONUNBUFFERED=1
ENV MAVEN_CONFIG=/home/ratass/.m2

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates \
        python3 \
        python3-pip \
        python3-venv \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace/ratass

COPY tools/rl/requirements.txt /tmp/ratass-rl-requirements.txt
RUN python3 -m venv /opt/ratass-rl-venv \
    && /opt/ratass-rl-venv/bin/pip install --upgrade pip \
    && /opt/ratass-rl-venv/bin/pip install -r /tmp/ratass-rl-requirements.txt

CMD ["bash", "tools/rl/docker_train_entrypoint.sh"]
