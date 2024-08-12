// these have two values, a timestamp and a byte array
record H264Packet
{
    public uint Timestamp { get; set; }
    public required byte[] Data { get; set; }
}
