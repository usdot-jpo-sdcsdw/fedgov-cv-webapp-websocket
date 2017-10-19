#!/bin/bash

sudo apt-get update
sudo apt-get -y install openjdk-6-jdk
sudo apt-get -y install unzip
sudo wget http://download.eclipse.org/jetty/8.1.16.v20140903/dist/jetty-distribution-8.1.16.v20140903.zip
sudo unzip jetty-distribution-8.1.16.v20140903.zip

sudo mv jetty-distribution-8.1.16.v20140903 jetty
sudo mv jetty /opt
sudo cp /opt/jetty/bin/jetty.sh /etc/init.d/jetty
sudo useradd jetty -M -U -s /bin/false
sudo chown -R jetty:jetty /opt/jetty

JettySettings=$(cat<<EOF
JAVA=/usr/bin/java # Path to Java
NO_START=0 # Start on boot
JETTY_HOST=0.0.0.0 # Listen to all hosts
#JETTY_ARGS=jetty.port=8080
JETTY_USER=jetty # Run as this user
JETTY_HOME=/opt/jetty
EOF
)

sudo echo "$JettySettings" > jetty
sudo mv jetty /etc/default

CN=`ec2metadata | grep public-hostname | cut -d ":" -f2 | cut -d " " -f2`

sudo keytool -genkey -noprompt \
 -alias jetty \
 -dname "CN=$CN, OU=DE, O=Leidos, L=, S=, C=US" \
 -keystore keystore \
 -keyalg RSA \
 -keysize 2048 \
 -storepass password \
 -keypass password \
 -validity 3650

echo "Generated key"
sudo keytool -list -v -keystore keystore -storepass password -keypass password

sudo cp keystore /opt/jetty/etc
sudo cp jetty.xml /opt/jetty/etc

sudo /sbin/iptables -t nat -I PREROUTING -p tcp --dport 443 -j REDIRECT --to-port 8443
sudo service jetty start