FROM ubuntu:18.04

MAINTAINER Fadeout Software <info@fadeout.it>

# Install Java and common tools
RUN apt-get update && apt-get install -y \
	openjdk-8-jre \
	software-properties-common \
	locate \
	nano

#Add Python 3.x Repo
RUN add-apt-repository -y ppa:deadsnakes/ppa

# Install Python 3.7
RUN apt-get update && apt-get install -y \
	python3.7

RUN apt-get update && apt-get install -y python3.7-dev

RUN mkdir /home/wasdi && chmod o+wrx /home/wasdi

# Go at Home and copy files
WORKDIR "/home/wasdi"
COPY ./get-pip.py /home/wasdi/

# Install PIP
RUN /usr/bin/python3.7 /home/wasdi/get-pip.py

# Install Flask
RUN pip install flask

# Install gunicorn
RUN pip install gunicorn

# Install Requests
RUN pip install requests

# Install Numpy
RUN pip install numpy

# Install Pandas
RUN pip install pandas

#Add Python 3.4 Repo
RUN add-apt-repository -y ppa:ubuntugis/ppa  && apt-get update && apt-get update

# Install GDAL
RUN apt-get install -y build-essential libssl-dev libffi-dev libxml2-dev libxslt1-dev zlib1g-dev
RUN apt-get install -y gdal-bin 
RUN apt-get install -y libgdal-dev 

RUN export CPLUS_INCLUDE_PATH=/usr/include/gdal
RUN export C_INCLUDE_PATH=/usr/include/gdal

RUN /usr/bin/python3.7 -m pip install --global-option=build_ext --global-option="-I/usr/include/gdal" GDAL==`gdal-config --version`

#TODO DA PROVARE
RUN apt-get update && apt install -y python-gdal

# Install Rasterio
#RUN pip install rasterio


#CREATE PYTHON LINK
RUN ln -sf /usr/bin/python3.7 /usr/bin/python

# Copy WASDI Server Files
COPY ./ /home/wasdi/

# Install Wasdi
RUN pip install wasdi

# Install User packages
RUN apt-get install -y `cat /home/wasdi/packages.txt | tr "\n" " "`; exit 0

# Install User Python Libs
RUN pip install `cat /home/wasdi/pip.txt | tr "\n" " "`; exit 0

# Set working dir
WORKDIR /home/wasdi

# Start the server
RUN chmod 777 ./runServer.sh
CMD ./runServer.sh
