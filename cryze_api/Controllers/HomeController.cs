using Microsoft.AspNetCore.Mvc;

namespace Cryze.API.Controllers;

[Route("/")]
public class HomeController : Controller
{
  public IActionResult Index()
  {
    return View();
  }
}
