
echo "Generating client code from swagger.yaml file..."
current_dir=$(pwd)
melophony_webapp_dir=$(dirname "$current_dir")
api_dir=$melophony_webapp_dir/api
echo $current_dir
echo $melophony_webapp_dir


wget http://localhost:1804/swagger.yaml -O $api_dir/melophony_api.yaml

sed -i '/format: date-time/d' ../api/melophony_api.yaml
sed -i '/id:/{n;n;n;d}' ../api/melophony_api.yaml
sed -i -E 's/(User|Artist|File|Track|Playlist):/\1:\n    x-implements: ["com.benlulud.melophony.database.IModel"]/g' ../api/melophony_api.yaml

rm -r $melophony_webapp_dir/app/src/main/java/com/benlulud/melophony/api
java -jar openapi/openapi-generator-cli-6.6.0.jar generate -i $api_dir/melophony_api.yaml -g java \
-o $melophony_webapp_dir/output_dir -c openapi/client_configuration.json

rsync -rtvu $melophony_webapp_dir/output_dir/src/main/java $melophony_webapp_dir/app/src/main/
rm -r $melophony_webapp_dir/output_dir