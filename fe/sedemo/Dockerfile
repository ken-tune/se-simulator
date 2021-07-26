# pull official base image
FROM node:14 as build

RUN git clone -b javalin-0-1 https://github.com/ken-tune/se-simulator.git

RUN cd se-simulator/fe/sedemo/
# set working directory
WORKDIR se-simulator/fe/sedemo/

ENV PATH se-simulator/fe/sedemo/node_modules/.bin:$PATH
# install app dependencies
RUN npm install --silent
RUN npm install react-scripts@3.4.1 -g --silent
RUN npm run build

FROM nginx:1.16.0-alpine
COPY --from=build se-simulator/fe/sedemo/build/ /usr/share/nginx/html
COPY nginx/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

