using System;
using System.Collections.Generic;

namespace MyRecordCollection.Models
{
    public class Composer
    {
        public Guid Id { get; set; }

        public string Title { get; set; }

        public string ImageUrl { get; set; }

        public List<Plate> Plates { get; set; }
    }
}
