using System.Text.Json;

// just a dictionary<string,dictionary<string,string>> that is locked when adding or getting messages
namespace Cryze.API.ViewModels
{
  public class CameraMessageViewModel
  {
    private readonly Dictionary<string, Dictionary<string, string>> _cameraMessages = new Dictionary<string, Dictionary<string, string>>();
    public CameraMessageViewModel(Dictionary<string, Dictionary<string, string>> cameraMessages)
    {
      _cameraMessages = cameraMessages;
    }

    public List<string> Devices => _cameraMessages.Keys.ToList();

    /// <summary>
    /// messages for a specific camera
    /// </summary>
    /// <param name="key">cameraId</param>
    /// <returns>Dictionary of messages, key is message type, value is raw json</returns>
    public Dictionary<string, string> this[string key]
    {
      get => _cameraMessages[key];
    }

    // TODO: this is a mess. I'd like to fix the datamodel so that this isn't necessary, i.e. nesting the json objects properly
    public string GetJson()
    {
      // the deepest level of the dictionary is already json. we only want to serialize the top level

      // break out the inner strings on the third level
      var json = JsonSerializer.Serialize(_cameraMessages);
      // find the beginning and end of the inner strings. they are always in quotes
      json = json.Replace("\\u0022", "\"");
      // remove the quotes
      json = json.Replace(":\"{\"", ":{\"");
      json = json.Replace("}\"", "}");

      // now, find ::. This is the beginning of a new json object. start a new object. find the end the object, and close it
      // find the end of the object by counting the number of open and close brackets
      // it looks like this "MSG_TYPE_PRO_READONLY::ProReadonly.power":
      // we want to change it to {"MSG_TYPE_PRO_READONLY":{"ProReadonly.power":
      // we want to change it to {"MSG_TYPE_PRO_READONLY":{"ProReadonly.power":<remaining string>}
      // {"MSG_TYPE_PRO_READONLY::ProReadonly.power":{"t":1726021963,"stVal":{"mode":1,"charging":0,"battery":84}}} -> {"MSG_TYPE_PRO_READONLY":{"ProReadonly.power":{"t":1726021963,"stVal":{"mode":1,"charging":0,"battery":84}}}}

      var original = JsonSerializer.Deserialize<Dictionary<string, Dictionary<string, object>>>(json);
      var result = new List<Dictionary<string, object>>();

      foreach (var property in original?? Enumerable.Empty<KeyValuePair<string, Dictionary<string, object>>>())
      {
        var newObject = new Dictionary<string, object>();
        var innerObject = new Dictionary<string, object>();

        foreach (var subProperty in property.Value)
        {
          if (subProperty.Key.Contains("::"))
          {
            var splitKey = subProperty.Key.Split(new[] { "::" }, StringSplitOptions.None);
            if (!innerObject.ContainsKey(splitKey[0]))
            {
              innerObject[splitKey[0]] = new Dictionary<string, object>();
            }
              ((Dictionary<string, object>)innerObject[splitKey[0]])[splitKey[1]] = subProperty.Value;
          }
          else
          {
            innerObject[subProperty.Key] = subProperty.Value;
          }
        }

        newObject[property.Key] = innerObject;
        result.Add(newObject);
      }
      return JsonSerializer.Serialize(result, new JsonSerializerOptions { WriteIndented = true });
    }
  }
}