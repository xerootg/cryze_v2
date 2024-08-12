# Base image
FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build

# Set the working directory
WORKDIR /app

# Copy the project file and restore dependencies
COPY *.csproj .
RUN dotnet restore

# Copy the source code and build the application
COPY . .
RUN dotnet publish -c Release -o out

# Runtime image
FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS runtime

# Install Python 3.11
RUN apt-get update -y && apt-get install -y python3.11 python3-pip

# Set the working directory
WORKDIR /app

COPY ./python /app/python
RUN pip install -r /app/python/requirements.txt --break-system-packages

# Copy the published output from the build stage
COPY --from=build /app/out .

# Start the application
ENTRYPOINT ["dotnet", "cryze_api.dll"]