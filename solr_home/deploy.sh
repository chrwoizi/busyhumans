#!/bin/sh
chgrp -R tomcat7 /home/ubuntu/solr_home
sudo mkdir /var/lib/solr &> /dev/null
sudo chown tomcat7 /var/lib/solr
sudo chgrp tomcat7 /var/lib/solr
mv -f /home/ubuntu/solr_home/* /var/lib/solr/
rm -R /home/ubuntu/solr_home